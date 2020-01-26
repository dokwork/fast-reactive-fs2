package ru.dokwork.fs2

import java.util.concurrent.atomic.AtomicReference

import cats.effect.{ CancelToken, Sync, Timer }
import cats.implicits._
import fs2.{ Chunk, Stream }
import org.reactivestreams.{ Publisher, Subscriber, Subscription }

import scala.collection.mutable
import scala.concurrent.duration._

trait StreamSubscriber[F[_], A] extends Subscriber[A] {
  def poll: F[Chunk[A]]
  def stream: Stream[F, A]
  def cancel: CancelToken[F]
}

object StreamSubscriber {

  /**
    * [[ru.dokwork.fs2.StreamSubscriber#create(int, scala.concurrent.duration.FiniteDuration, cats.effect.Sync, cats.effect.Timer) Creates subscriber]]
    * and subscribes it to the event from the publisher.
    *
    * @return stream elements from the publisher.
    */
  def subscribe[F[_]: Sync: Timer, A](
      publisher: Publisher[A],
      chunkSize: Int = 1000,
      awaitNextTimeout: FiniteDuration = 100.millis
  ): Stream[F, A] =
    Stream
      .eval(create[F, A](chunkSize, awaitNextTimeout))
      .evalTap(s => Sync[F].delay(publisher.subscribe(s)))
      .flatMap(_.stream)

  /**
    * Creates subscriber which put every message to the buffer.
    * This buffer will be pushed to the stream as single chunk. Every polling from
    * the empty buffer will be paused for `awaitNextTimeout`.
    */
  def create[F[_]: Sync: Timer, A](
      chunkSize: Int = 1000,
      awaitNextTimeout: FiniteDuration = 100.millis
  ): F[StreamSubscriber[F, A]] =
    Sync[F].delay(new StreamSubscriber[F, A] {
      private val queue = new ChunkQueue[A]
      private val fsm   = new FSM[A](chunkSize)

      override def onSubscribe(s: Subscription): Unit = neNull(s) {
        fsm.subscribe(s) match {
          // cancel new subscription (see spec for reactive streams)
          case fsm.Subscribed(sub) if s ne sub => s.cancel()
          case _                               =>
        }
      }
      override def onNext(a: A): Unit = neNull(a) { queue.put(a) }

      override def onError(e: Throwable): Unit = neNull(e) { fsm.raise(e) }

      override def onComplete(): Unit = fsm.complete()

      private def neNull[R](x: Any)(f: => R): R = if (x equals null) throw new NullPointerException else f

      def poll: F[Chunk[A]] =
        Sync[F].delay(queue.popAll).flatMap {
          chunk =>
            fsm.poll(chunk) match {
              // poll last chunk
              case fsm.Completed | fsm.Canceled => Sync[F].pure(chunk)
              // first request
              case fsm.Idle(sub) if chunk.isEmpty => Sync[F].delay(sub.request(chunkSize)) >> poll
              // next request
              case fsm.Idle(sub) => Sync[F].delay(sub.request(chunkSize)) as chunk
              // raise error
              case fsm.Error(e) => Sync[F].raiseError(e)
              // produce non empty chunk
              case _ if chunk.nonEmpty => Sync[F].pure(chunk)
              // queue is empty. wait and retry
              case _ => Timer[F].sleep(awaitNextTimeout) >> poll
            }
        }

      def stream: Stream[F, A] =
        Stream.eval(poll).repeat.takeWhile(_.nonEmpty).flatMap(Stream.chunk).onFinalize(cancel)

      def cancel: CancelToken[F] = Sync[F].delay {
        fsm.cancel() match {
          case fsm.Cancel(sub) => sub.cancel()
          case _               =>
        }
      }
    })

  /** Side-effect-free fsm of the subscriber. */
  private class FSM[A](chunkSize: Int) {

    sealed trait State

    case object Unsubscribed extends State

    case class Subscribed(sub: Subscription) extends State

    case class Idle(sub: Subscription) extends State

    case class Await(sub: Subscription, awaitCount: Int) extends State

    case class Error(e: Throwable) extends State

    case class Cancel(sub: Subscription) extends State

    case object Canceled extends State

    case object Completed extends State

    private val ref = new AtomicReference[State](Unsubscribed)

    def subscribe(sub: Subscription): State = ref.updateAndGet {
      case Unsubscribed           => Subscribed(sub)
      case subscribed: Subscribed => subscribed
      case Canceled               => Canceled
      case state                  => Error(new IllegalStateException(s"Unexpected state on `subscribe`: $state"))
    }

    def poll(chunk: Chunk[A]): State = ref.updateAndGet {
      case Subscribed(sub)                                   => Idle(sub)
      case Idle(sub)                                         => Await(sub, chunkSize)
      case Await(sub, awaitCount) if awaitCount > chunk.size => Await(sub, awaitCount - chunk.size)
      case Await(sub, _)                                     => Idle(sub)
      case other                                             => other
    }

    def raise(e: Throwable): State = ref.updateAndGet(_ => Error(e))

    def cancel(): State = ref.updateAndGet {
      case Subscribed(sub)                           => Cancel(sub)
      case Idle(sub)                                 => Cancel(sub)
      case Await(sub, _)                             => Cancel(sub)
      case Unsubscribed                              => Canceled
      case Cancel(_)                                 => Canceled // to avoid double cancel
      case other @ (Error(_) | Canceled | Completed) => other
    }

    def complete(): State = ref.updateAndGet {
      case error: Error => error
      case _            => Completed
    }

    def nonCompleted: Boolean = ref.get() != Completed
  }

  private class ChunkQueue[A] extends AtomicReference[mutable.Buffer[A]](mutable.Buffer.empty[A]) {
    def put(a: A): Unit = updateAndGet(_ += a)

    def popAll: Chunk[A] = Chunk.buffer(getAndUpdate(_ => mutable.Buffer.empty[A]))
  }

}
