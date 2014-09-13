import java.nio.file.{Files, Paths}

import sbt.Keys._
import sbt._

object TestBuild extends Build {
  val checkSymlink = taskKey[Unit]("Check symlink of the runtime_resources folder")
  val checkUnityProject = taskKey[Unit]("Check that Unity created a project in the folder (check Library and ProjectSettings existence)")

  def testSettings:Seq[Setting[_]] = Seq(
      checkSymlink := {
        for(dirname <- List(normalizedName.value, normalizedName.value + "_test")) {
          val path = Paths.get(target.value.getAbsolutePath + s"/test-workspace/Assets/$dirname");
          if(!Files.isSymbolicLink(path)) {
            sys.error(s"$path is not a symbolic link");
          }
        }
      },
      checkUnityProject := {
        if (!(target.value / s"/test-workspace/Library/").exists()
          || !(target.value / s"/test-workspace/ProjectSettings/").exists()) {
          sys.error("Unity project creation failed, (missing Library or ProjectSettings folders)");
        }
      }
    )
}

