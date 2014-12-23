import sbt._

object NativeBuilderBuild extends Build {
  lazy val root = Project(id = "native-builder",
    base = file(".")) aggregate(core)

  lazy val core = Project(id = "core",
    base = file("core"))
}
