import java.nio.file.Files

import sbt.Keys._
import sbt._

object TestBuild extends Build {
  val checkSettingFolder = taskKey[Unit]("Check symlinking of settings file")

  def testSettings:Seq[Setting[_]] = Seq(
    checkSettingFolder := {
      for (settingFile <- ((sourceDirectory in Test).value / "unity_settings").listFiles("*.asset")) {
        val linkedTarget = target.value / s"/test-workspace/ProjectSettings/${settingFile.name}";
        if(!Files.isSymbolicLink(linkedTarget toPath)) {
          sys.error(s"$linkedTarget is not a symbolic link");
        }
      }
    }
  )
}

