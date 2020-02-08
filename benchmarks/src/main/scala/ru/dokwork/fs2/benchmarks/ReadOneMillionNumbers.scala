package ru.dokwork.fs2.benchmarks

import java.util.concurrent.TimeUnit

import cats.effect.{ ConcurrentEffect, ContextShift, IO, Timer }
import org.openjdk.jmh.annotations._
import org.reactivestreams.Subscriber
import org.reactivestreams.example.unicast.RangePublisher

import scala.concurrent.ExecutionContext.Implicits.global

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class ReadOneMillionNumbers {
  implicit val timer: Timer[IO]          = IO.timer(global)
  implicit val cs: ContextShift[IO]      = IO.contextShift(global)
  implicit val eff: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  val publisher = new RangePublisher(0, Int.MaxValue)

  @Benchmark
  def fs2StreamSubscriber(): Unit =
    readOneMillionNumbers(fs2.interop.reactivestreams.StreamSubscriber.apply[IO, Integer])(_.stream).unsafeRunSync()

  @Benchmark
  def dokworkStreamSubscriber(): Unit =
    readOneMillionNumbers(ru.dokwork.fs2.StreamSubscriber.create[IO, Integer]())(_.stream).unsafeRunSync()

  def readOneMillionNumbers[S <: Subscriber[Integer]](subscriber: IO[S])(asStream: S => fs2.Stream[IO, Integer]): IO[Unit] = {
    for {
      subs <- subscriber
      _    = publisher.subscribe(subs)
      _    <- asStream(subs).take(1000000).compile.drain
    } yield ()
  }
}
