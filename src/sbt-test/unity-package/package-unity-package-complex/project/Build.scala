/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
import sbt._
import sbt.complete.Parsers._

import scala.io.Source

object TestBuild extends Build {
  val existsInFile = inputKey[Unit]("Check if a string exists in files")

  def testSettings:Seq[Setting[_]] = Seq(
    existsInFile := {
      val args = spaceDelimited("<paths>").parsed;
      if(args.size > 1) {
        val searchIndex = args drop 1 map { file(_) } flatMap allSubFiles filter { _.name == "pathname" } indexWhere { file =>
          val src = Source.fromFile(file)
          val hit = src getLines() exists { _.contains(args(0)) }
          src.close

          hit;
        }

        if (searchIndex == -1) {
          sys.error(s"${args(0)} was not found");
        }
      }
      else {
        sys.error("Usage: <expression> <roots to search...>")
      }
    }
  )

  private def allSubFiles(f:File):Seq[File] = {
    if (f.isFile)
      return Seq(f)
    else
      return f listFiles() flatMap { allSubFiles(_) }
  }
}

