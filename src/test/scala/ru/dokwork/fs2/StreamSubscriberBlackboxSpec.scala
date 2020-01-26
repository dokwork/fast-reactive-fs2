package ru.dokwork.fs2

import java.util.concurrent.atomic.AtomicInteger

import cats.effect._
import org.reactivestreams._
import org.reactivestreams.tck._
import org.scalatestplus.testng.TestNGSuiteLike
import ru.dokwork.fs2.StreamSubscriber.StreamSubscriberImpl

import scala.concurrent.ExecutionContext

final class StreamSubscriberBlackboxSpec
    extends SubscriberBlackboxVerification[Int](new TestEnvironment(1000L))
    with TestNGSuiteLike {
  implicit val timer                 = IO.timer(ExecutionContext.global)
  implicit val ctx: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  private val counter = new AtomicInteger()

  def createElement(i: Int): Int = counter.incrementAndGet()

  def createSubscriber(): StreamSubscriberImpl[IO, Int] =
    new StreamSubscriberImpl[IO, Int]()

  override def triggerRequest(s: Subscriber[_ >: Int]): Unit =
    s.asInstanceOf[StreamSubscriberImpl[IO, Int]].stream.compile.drain.unsafeRunAsync(_ => ())
}
