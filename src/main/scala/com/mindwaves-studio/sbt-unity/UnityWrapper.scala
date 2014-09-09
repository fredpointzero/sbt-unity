package com.mindwaves_studio.sbt_unity

import sbt._

/**
 * Created by Fredpointzero on 09/09/2014.
 */
object UnityWrapper {
  import scala.sys.process._

  object UnityBuildTarget extends Enumeration {
    type UnityBuildTarget = Value;
    val Windows, Windows64, OSX, OSX64, OSXUniversal, Linux32, Linux64, LinuxUniversal, Web, WebStreamed, None = Value;
  }

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

  def getBuildTargetCapabilitiesFromOS(osName:String):Seq[UnityBuildTarget.Value] = {
    osName toLowerCase match {
      case WindowsPattern(c) => Seq(UnityBuildTarget.Windows, UnityBuildTarget.Windows64, UnityBuildTarget.Web, UnityBuildTarget.WebStreamed);
      case OSXPattern(c) => Seq(UnityBuildTarget.OSX, UnityBuildTarget.OSX,  UnityBuildTarget.OSXUniversal, UnityBuildTarget.Web, UnityBuildTarget.WebStreamed);
      case _ => Seq(UnityBuildTarget.None);
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

  def buildUnityPlayer(projectPath: File, logFile: File, targetPlatform:UnityBuildTarget.Value, targetDirectory:File) = {
    val buildCapabilities = getBuildTargetCapabilitiesFromOS(System.getProperty("os.name"));
    if (!buildCapabilities.contains(targetPlatform)) {
      throw new IllegalArgumentException(s"Target platform $targetPlatform is not supported on this OS");
    }

    val buildMethod = targetPlatform match {
      case UnityBuildTarget.Linux32 => "buildLinux32Player";
      case UnityBuildTarget.Linux64 => "buildLinux64Player";
      case UnityBuildTarget.LinuxUniversal => "buildLinuxUniversalPlayer";
      case UnityBuildTarget.OSX => "buildOSXPlayer";
      case UnityBuildTarget.OSX64 => "buildOSX64Player";
      case UnityBuildTarget.OSXUniversal => "buildOSXUniversalPlayer";
      case UnityBuildTarget.Web => "buildWebPlayer";
      case UnityBuildTarget.WebStreamed => "buildWebPlayerStreamed";
      case UnityBuildTarget.Windows => "buildWindowsPlayer";
      case UnityBuildTarget.Windows64 => "buildWindows64Player";
      case _ => throw new IllegalArgumentException(s"Unmanaged build target: $targetPlatform");
    }

    val result = Seq(
      detectUnityExecutable.getAbsolutePath(),
      "-batchMode",
      "-quit",
      "-logFile",
      logFile.getAbsolutePath(),
      "-projectPath ",
      projectPath.getAbsolutePath(),
      s"-$buildMethod",
      targetDirectory.getAbsolutePath()).!;

    if(result != 0) {
      throw new RuntimeException(s"Could not build player Unity project at $projectPath (see $logFile)");
    }
  }

  private val WindowsPattern = "(.*win.*)".r;
  private val OSXPattern = "(.*mac.*)".r;
}
