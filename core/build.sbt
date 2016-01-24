name := "core"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "3.0.0-SNAP4" % "test"

libraryDependencies += "com.twitter" %% "util-core" % "6.22.0"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.3"

libraryDependencies += "org.mockito" % "mockito-all" % "1.10.8" % "test"

libraryDependencies ++= Seq(
  "io.spray" %% "spray-can" % "1.3.1" % "test",
  "io.spray" %% "spray-httpx" % "1.3.1",
  "io.spray" %% "spray-client" % "1.3.1"
)

libraryDependencies += "org.kamranzafar" % "jtar" % "2.2"
