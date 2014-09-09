package com.mindwaves_studio.sbt_unity

import sbt._

/**
 * Created by Fredpointzero on 09/09/2014.
 */
object UnityWrapper {
  import scala.sys.process._

  private val WindowsPattern = "(.*win.*)".r;
  private val OSXPattern = "(.*mac.*)".r;

  val UNITY_EXECUTABLE_SYSTEM_PROPERTY = "UNITY_EDITOR_PATH";

  def detectUnityExecutable = {
    val systemUnityExecutable = System.getProperty(UNITY_EXECUTABLE_SYSTEM_PROPERTY);
    var result:File = null;
    if (systemUnityExecutable != null) {
      result = file(systemUnityExecutable);
    }
    else {
      result = detectUnityExecutableFromOS(System.getProperty("os.name"));
    }

    if (!result.canExecute()) {
      throw new RuntimeException(s"The Unity editor application ($result) cannot be executed");
    }

    result;
  }

  def detectUnityExecutableFromOS(osName:String) = {
    osName toLowerCase match {
      case WindowsPattern(c) => file("C:\\Program Files (x86)\\Unity\\Editor\\Unity.exe");
      case OSXPattern(c) => file("/Applications/Unity/Unity.app/Contents/MacOS/Unity");
      case _ => throw new RuntimeException(s"This OS ($osName) is not managed by Unity Editor");
    }
  }

  def createUnityProjectAt(projectPath:File, logFile:File) = {
    val result = Seq(
      detectUnityExecutable.getAbsolutePath(),
      "-batchMode",
      "-quit",
      "-logFile",
      logFile.getAbsolutePath(),
      "-createProject",
      projectPath.getAbsolutePath()).!;

    if(result != 0) {
      throw new RuntimeException(s"Could not create Unity project at $projectPath (see $logFile)");
    }
  }
}
