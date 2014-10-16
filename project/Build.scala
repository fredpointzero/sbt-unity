/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
import sbt._
import sbt.complete.Parsers._

import scala.io.Source

object ThisBuild extends Build {

  val OSName = System.getProperty("os.name").toLowerCase();

  val integrationTestAnyPlatform = settingKey[String]("Integration tests that can run on any platform");
  val integrationTestWindowsOnly = settingKey[String]("Integration tests that run only on Windows");
  val integrationTestWindows = settingKey[String]("Integration tests that run on Windows");
  val integrationTestOSXOnly = settingKey[String]("Integration tests that run only on OSX");
  val integrationTestOSX = settingKey[String]("Integration tests that run on OSX");
  val integrationTestOnThisPlatform = settingKey[String]("Integration tests that run on this platform");
  val integrationTest = taskKey[Unit]("Run integration tests");

  val integrationTestTask = Def.taskDyn {
    ScriptedPlugin.scripted.toTask(" " + integrationTestOnThisPlatform.value)
  }

  def thisBuildSettings:Seq[Setting[_]] = Seq(
    integrationTestWindows := integrationTestAnyPlatform.value + " " + integrationTestWindowsOnly.value,

    integrationTestOSX := integrationTestAnyPlatform.value + " " + integrationTestOSXOnly.value,

    integrationTestOnThisPlatform := {
      if (OSName.indexOf("win") >= 0) {
        integrationTestWindows.value;
      } else if (OSName.indexOf("mac") >= 0) {
        integrationTestOSX.value;
      } else {
        integrationTestAnyPlatform.value;
      }
    },

    integrationTest := {
      integrationTestTask.value;
    }
  );
}

