val scala3Version = "3.5.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "gears-batteries",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "ch.epfl.lamp" %% "gears" % "0.2.0",
    libraryDependencies += "com.lihaoyi" %% "requests" % "0.9.0",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "4.0.2",
    libraryDependencies += "com.lihaoyi" %% "pprint" % "0.9.0",
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
  )
