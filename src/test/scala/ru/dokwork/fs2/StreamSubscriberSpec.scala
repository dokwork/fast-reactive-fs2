package ru.dokwork.fs2

import cats.effect.{ ContextShift, IO }
import org.reactivestreams.example.unicast.RangePublisher
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class StreamSubscriberSpec extends AnyFlatSpec with Matchers {

  implicit val timer                 = IO.timer(ExecutionContext.global)
  implicit val ctx: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "stream" should "produce specified count of elements" in {
    // given:
    val N         = 1000
    val publisher = new RangePublisher(0, Int.MaxValue)
    // when:
    val stream = StreamSubscriber.subscribe[IO, Integer](publisher, chunkSize = 10).take(N)
    // then:
    stream.compile.toList.unsafeRunTimed(5.seconds) should contain((0 until N).toList)
  }

  "every subscription" should "create new stream" in {
    // given:
    val N         = 1000
    val publisher = new RangePublisher(0, Int.MaxValue)
    // when:
    val stream1 = StreamSubscriber.subscribe[IO, Integer](publisher, chunkSize = 10)
    val stream2 = StreamSubscriber.subscribe[IO, Integer](publisher, chunkSize = 10)
    val all = stream1.zip(stream2).take(N)
    // then:
    all.compile.toList.unsafeRunTimed(5.seconds) should contain((0 until N).zipWithIndex.toList)
  }
  "stream" should "be reference transparent" in {
    // given:
    val N         = 1000
    val publisher = new RangePublisher(0, Int.MaxValue)
    // when:
    val stream = StreamSubscriber.subscribe[IO, Integer](publisher, chunkSize = 10)
    val all = stream.zip(stream).take(N)
    // then:
    all.compile.toList.unsafeRunTimed(5.seconds) should contain((0 until N).zipWithIndex.toList)
  }
}
