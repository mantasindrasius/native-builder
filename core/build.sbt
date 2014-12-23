name := "core"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0.M5b" % "test"

libraryDependencies += "com.twitter" %% "util-core" % "6.22.0"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.0"

libraryDependencies ++= Seq(
  "io.spray" % "spray-can" % "1.3.1" % "test",
  "io.spray" % "spray-httpx" % "1.3.1",
  "io.spray" % "spray-client" % "1.3.1"
)

libraryDependencies += "org.kamranzafar" % "jtar" % "2.2"
