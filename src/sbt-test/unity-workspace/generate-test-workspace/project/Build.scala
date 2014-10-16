/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
import java.nio.file.{Files, Paths}

import sbt._
import sbt.complete.Parsers._

object TestBuild extends Build {
  val isSymlink = inputKey[Unit]("Check symlink of the runtime_resources folder")

  def testSettings:Seq[Setting[_]] = Seq(
    isSymlink := {
      for(pathString:String <- spaceDelimited("<paths>").parsed) {
        val path = Paths.get(pathString);
        if(!Files.isSymbolicLink(path)) {
          sys.error(s"$path is not a symbolic link");
        }
      }
    }
  )
}

