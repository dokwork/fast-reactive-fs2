lazy val scala213 = "2.13.1"
lazy val scala212 = "2.12.10"
lazy val scala211 = "2.11.12"

lazy val `fast-reactive-fs2` = (project in file("."))
  .settings(
    organization := "ru.dokwork",
    scalaVersion := scala213,
    crossScalaVersions := Seq(scala211, scala212, scala213),
    scalacOptions ++= Seq(
      "-encoding",
      "utf-8",
      "-target:jvm-1.8",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Xfatal-warnings",
      "-language:higherKinds"
    ),
    libraryDependencies ++= Seq(
      "org.typelevel"       %% "cats-core"       % "2.0.0",
      "org.typelevel"       %% "cats-effect"     % "2.0.0",
      "co.fs2"              %% "fs2-core"        % "2.0.0",
      "org.reactivestreams" % "reactive-streams" % "1.0.3",
      // tests:
      "org.scalatest"       %% "scalatest"            % "3.1.0"    % "test",
      "org.scalatestplus"   %% "scalatestplus-testng" % "1.0.0-M2" % "test",
      "org.reactivestreams" % "reactive-streams-tck"  % "1.0.3"    % "test"
    )
  )
  .settings(
    coverageMinimum := 88,
    coverageFailOnMinimum := true
  )

lazy val benchmarks = (project in file("benchmarks"))
  .enablePlugins(JmhPlugin)
  .settings(
    organization := "ru.dokwork",
    scalaVersion := scala213,
    libraryDependencies ++= Seq(
      "org.reactivestreams" % "reactive-streams-tck"  % "1.0.3",
      "co.fs2"              %% "fs2-reactive-streams" % "2.2.1"
    )
  )
  .dependsOn(`fast-reactive-fs2`)
