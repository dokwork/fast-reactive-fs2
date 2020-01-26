lazy val `fast-reactive-fs2` = (project in file("."))
  .settings(
    organization := "ru.dokwork",
    scalaVersion := "2.13.1",
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
      "org.typelevel" %% "cats-core"   % "2.0.0",
      "org.typelevel" %% "cats-effect" % "2.0.0",
      "co.fs2"        %% "fs2-core"    % "2.0.0",
      "org.reactivestreams" % "reactive-streams" % "1.0.3",
      // tests:
      "org.scalatest" %% "scalatest" % "3.1.0" % "test",
      "org.reactivestreams" % "reactive-streams-tck" % "1.0.3" % "test",
      "org.scalatestplus" %% "scalatestplus-testng" % "1.0.0-M2" % "test"
    )
  )
  .settings(
    coverageMinimum := 90,
    coverageFailOnMinimum := true
  )