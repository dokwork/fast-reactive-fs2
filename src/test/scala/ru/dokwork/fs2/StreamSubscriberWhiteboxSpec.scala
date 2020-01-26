package ru.dokwork.fs2

import java.util.concurrent.atomic.AtomicInteger

import cats.effect.{ ContextShift, IO, Timer }
import cats.implicits._
import org.reactivestreams.tck.SubscriberWhiteboxVerification.{ SubscriberPuppet, WhiteboxSubscriberProbe }
import org.reactivestreams.tck.{ SubscriberWhiteboxVerification, TestEnvironment }
import org.reactivestreams.{ Subscriber, Subscription }
import org.scalatestplus.testng.TestNGSuiteLike
import ru.dokwork.fs2.StreamSubscriber.StreamSubscriberImpl

import scala.concurrent.ExecutionContext

final class SubscriberWhiteboxSpec
    extends SubscriberWhiteboxVerification[Int](new TestEnvironment(1000L))
    with TestNGSuiteLike {
  implicit val timer: Timer[IO]      = IO.timer(ExecutionContext.global)
  implicit val ctx: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  private val counter = new AtomicInteger()

  def createSubscriber(p: WhiteboxSubscriberProbe[Int]): Subscriber[Int] =
    new WhiteboxSubscriber(new StreamSubscriberImpl[IO, Int](), p)

  def createElement(i: Int): Int = counter.getAndIncrement
}

class WhiteboxSubscriber[A](sub: StreamSubscriberImpl[IO, A], probe: WhiteboxSubscriberProbe[A]) extends Subscriber[A] {
  def onError(t: Throwable): Unit = {
    sub.onError(t)
    probe.registerOnError(t)
  }

  def onSubscribe(s: Subscription): Unit = {
    sub.onSubscribe(s)
    probe.registerOnSubscribe(new SubscriberPuppet {
      override def triggerRequest(elements: Long): Unit =
        sub.poll.void.unsafeRunAsync(_ => ())

      override def signalCancel(): Unit =
        s.cancel()
    })
  }

  def onComplete(): Unit = {
    sub.onComplete()
    probe.registerOnComplete()
  }

  def onNext(a: A): Unit = {
    sub.onNext(a)
    probe.registerOnNext(a)
  }
}
