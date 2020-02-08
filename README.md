# fast-reactive-fs2
[![Build Status](https://travis-ci.com/dokwork/fasti.svg?branch=master)](https://travis-ci.com/dokwork/fasti)
[![Coverage Status](https://coveralls.io/repos/github/dokwork/fast-reactive-fs2/badge.svg?branch=master)](https://coveralls.io/github/dokwork/fast-reactive-fs2?branch=master)
 [ ![Download](https://api.bintray.com/packages/dokwork/maven/fast-reactive-fs2/images/download.svg) ](https://bintray.com/dokwork/maven/fast-reactive-fs2/_latestVersion)

Alternative realization of the reactive streams for fs2. 

This version is not cross-platform, but faster than official module:
```
Benchmark                                      Mode  Cnt      Score      Error  Units
ReadOneMillionNumbers.dokworkStreamSubscriber  avgt   25     39.377 ±    2.749  ms/op
ReadOneMillionNumbers.fs2StreamSubscriber      avgt   25  11189.737 ± 2213.225  ms/op
```

## Installation
`libraryDependencies += "ru.dokwork" %% "fast-reactive-fs2" % "0.1.0"`

## Usage
```scala
val publisher: Publisher[A] = ???
val stream: fs2.Stream[F, A] = StreamSubscriber.subscribe[F, A](publisher)
```