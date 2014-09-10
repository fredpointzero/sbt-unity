import java.nio.file.{Files, Paths}

import sbt.Keys._
import sbt._

object TestBuild extends Build {
  val checkSymlink = taskKey[Unit]("Check symlink of the runtime_resources folder")
  val checkUnityProject = taskKey[Unit]("Check that Unity created a project in the folder (check Library and ProjectSettings existence)")

  def testSettings:Seq[Setting[_]] =
    inConfig(Compile)(
      testSettings0 ++ Seq(
        checkSymlink in Compile <<= checkSymlinkIn(Compile, "main"),
        checkUnityProject in Compile <<= checkUnityProjectIn(Compile)
      )
    ) ++
    inConfig(Test)(
      testSettings0 ++ Seq(
        checkSymlink in Test <<= checkSymlinkIn(Test, "main"),
        checkUnityProject in Test <<= checkUnityProjectIn(Test)
      )
    )

  private def checkSymlinkIn(c:Configuration, alias:String) =
    (target, normalizedName) map { (targetDirectory, normName) =>
      val path = Paths.get(targetDirectory.getAbsolutePath + s"/unityBuild${c}Workspace/Assets/${normName}_$alias");
      if(!Files.isSymbolicLink(path)) {
        sys.error(s"$path is not a symbolic link");
      }
    }

  private def checkUnityProjectIn(c:Configuration) =
    (target, normalizedName) map { (targetDirectory, normName) =>
      if (!(targetDirectory / s"/unityBuild${c}Workspace/Library/").exists()
        || !(targetDirectory / s"/unityBuild${c}Workspace/ProjectSettings/").exists()) {
        sys.error("Unity project creation failed, (missing Library or ProjectSettings folders)");
      }
    }

  private def testSettings0: Seq[Setting[_]] = Seq(

  )
}

