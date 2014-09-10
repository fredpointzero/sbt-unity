import java.nio.file.Files

import sbt.Keys._
import sbt._

object TestBuild extends Build {
  val checkSettingFolder = taskKey[Unit]("Check symlinking of settings file")

  def testSettings:Seq[Setting[_]] =
    inConfig(Compile)(
      testSettings0 ++ Seq(
        checkSettingFolder in Compile <<= checkSettingFolderIn(Compile)
      )
    ) ++
    inConfig(Test)(
      testSettings0 ++ Seq(
        checkSettingFolder in Test <<= checkSettingFolderIn(Test)
      )
    )

  private def checkSettingFolderIn(c:Configuration) =
    (sourceDirectory in c, target, normalizedName) map { (baseDir, targetDirectory, normName) =>
      for (settingFile <- (baseDir / "unity_settings").listFiles("*.asset")) {
        val linkedTarget = targetDirectory / s"/unityBuild${c}Workspace/ProjectSettings/${settingFile.name}";
        if(!Files.isSymbolicLink(linkedTarget toPath)) {
          sys.error(s"$linkedTarget is not a symbolic link");
        }
      }
    }

  private def testSettings0: Seq[Setting[_]] = Seq(

  )
}

