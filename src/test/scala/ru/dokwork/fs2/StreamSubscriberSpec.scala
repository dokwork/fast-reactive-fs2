package ru.dokwork.fs2

import cats.effect.{ContextShift, IO}
import org.reactivestreams.example.unicast.RangePublisher
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class StreamSubscriberSpec extends AnyFlatSpec with Matchers {

  implicit val timer                 = IO.timer(ExecutionContext.global)
  implicit val ctx: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "stream" should "be canceled" in {
    // given:
    val publisher = new RangePublisher(0, Int.MaxValue)
    // when:
    val stream = StreamSubscriber.subscribe[IO, Integer](publisher, chunkSize = 10).take(1000)
    // then:
    stream.compile.drain.unsafeRunTimed(5.seconds) should not be empty
  }
}
