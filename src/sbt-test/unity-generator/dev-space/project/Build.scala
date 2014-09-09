import java.nio.file.{Files, Paths}

import sbt.Keys._
import sbt._

object TestBuild extends Build {
  val checkSymlink = taskKey[Unit]("Check symlink of the runtime_resources folder")
  val checkUnityProject = taskKey[Unit]("Check that Unity created a project in the folder (check Library and ProjectSettings existence)")

  def testSettings:Seq[Setting[_]] =
    testSettingsIn(Compile) ++ testSettingsIn(Test)

  private def testSettingsIn(c: Configuration): Seq[Setting[_]] =
    inConfig(c)(testSettings0 ++ Seq(
      checkSymlink in Compile <<= (target, normalizedName) map { (targetDirectory, normName) =>
        val path = Paths.get(targetDirectory.getAbsolutePath + s"/unityBuild${c}Workspace/Assets/${normName}_$c");
        if(!Files.isSymbolicLink(path)) {
          sys.error(s"$path is not a symbolic link");
        }
        ()
      },
      checkUnityProject <<= (target, normalizedName) map { (targetDirectory, normName) =>
        if (!(targetDirectory / s"/unityBuild${c}Workspace/Library/").exists()
          || !(targetDirectory / s"/unityBuild${c}Workspace/ProjectSettings/").exists()) {
          sys.error("Unity project creation failed, (missing Library or ProjectSettings folders)");
        }
        ()
      }
    ))

  private def testSettings0: Seq[Setting[_]] = Seq(

  )
}

