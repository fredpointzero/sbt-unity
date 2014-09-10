import sbt.Keys._
import sbt.ScriptedPlugin._
import sbt._

object TestBuild extends Build {
  lazy val root = Project("sbt-unity", file("."))
  .configs(RunDebug)
  .settings(inConfig(RunDebug)(Defaults.configTasks):_*)
  .settings(
    name := "sbt-unity debug",
    fork in RunDebug := true,
    scriptedLaunchOpts in RunDebug := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-XX:MaxHeapSize=512M", "-Dplugin.version=" + version.value, "-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:5006")
    }
  )

  lazy val RunDebug = config("debug").extend(Runtime)
}