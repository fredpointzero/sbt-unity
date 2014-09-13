package com.mindwaves_studio.sbt_unity

import sbt._

/**
 * Created by Fredpointzero on 09/09/2014.
 */
object UnityWrapper {
  import scala.sys.process._

  object TargetPlatform extends Enumeration {
    type TargetPlatform = Value;
    val Windows, Windows64, OSX, OSX64, OSXUniversal, Linux32, Linux64, LinuxUniversal, Web, WebStreamed, None = Value;
  }

  val UNITY_EXECUTABLE_SYSTEM_PROPERTY = "UNITY_EDITOR_PATH";

  def detectUnityExecutable = {
    val systemUnityExecutable = System.getenv(UNITY_EXECUTABLE_SYSTEM_PROPERTY);
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

  def getBuildTargetCapabilitiesFromOS(osName:String):Seq[TargetPlatform.Value] = {
    osName toLowerCase match {
      case WindowsPattern(c) => Seq(TargetPlatform.Windows, TargetPlatform.Windows64, TargetPlatform.Web, TargetPlatform.WebStreamed);
      case OSXPattern(c) => Seq(TargetPlatform.OSX, TargetPlatform.OSX,  TargetPlatform.OSXUniversal, TargetPlatform.Web, TargetPlatform.WebStreamed);
      case _ => Seq(TargetPlatform.None);
    }
  }

  def createUnityProjectAt(projectPath:File, logFile:File, log:Logger) = {
    val executable = detectUnityExecutable;
    log.info(s"Using $executable");
    val result = Seq(
      executable.getAbsolutePath(),
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

  def extensionForPlatform(target:TargetPlatform.Value) = platformMapping(target)._2;

  def buildUnityPlayer(projectPath: File, logFile: File, targetPlatform:TargetPlatform.Value, targetFile:File, log:Logger) = {
    val buildCapabilities = getBuildTargetCapabilitiesFromOS(System.getProperty("os.name"));
    if (!buildCapabilities.contains(targetPlatform)) {
      throw new IllegalArgumentException(s"Target platform $targetPlatform is not supported on this OS");
    }

    val (buildMethod, ext) = platformMapping(targetPlatform);
    if(buildMethod == null){
      throw new RuntimeException(s"Unmanaged target platform: $targetPlatform");
    }

    val parentDir = file(targetFile.getParent());
    if (!parentDir.exists()) {
      parentDir.mkdirs();
    }

    var targetPath = targetFile.getAbsolutePath();
    if (ext != null && !targetPath.endsWith(ext)) {
      targetPath += s".$ext";
    }

    val executable = detectUnityExecutable;
    log.info(s"Using $executable");
    val result = Seq(
      executable.getAbsolutePath(),
      "-batchMode",
      "-quit",
      "-logFile",
      logFile.getAbsolutePath(),
      "-projectPath ",
      projectPath.getAbsolutePath(),
      s"-$buildMethod",
      targetPath).!;

    if(result != 0) {
      throw new RuntimeException(s"Could not build Unity player at $projectPath (see $logFile)");
    }
  }

  def buildUnityPackage(projectPath: File, targetFile:File, logFile:File, sourceDirectories:Seq[String], log:Logger) = {

    val executable = detectUnityExecutable;
    log.info(s"Using $executable");
    val commonCmd = List(
      executable.getAbsolutePath(),
      "-batchMode",
      "-quit",
      "-projectPath ",
      projectPath.getAbsolutePath(),
      "-exportPackage"
    );

    val parentDirectory = file(targetFile.getParent());
    if (!parentDirectory.exists()) {
      parentDirectory.mkdirs();
    }

    // Check that inputs are directories
    for (path <- sourceDirectories) {
      val fileToCheck = projectPath / path;
      if (!fileToCheck.isDirectory()) {
        throw new RuntimeException(s"$path ($fileToCheck) is not a directory");
      }
    }

    val result = (commonCmd ++ (sourceDirectories :+ targetFile.toString()) ++ Seq("-logFile", logFile.toString())) !;

    if(result != 0) {
      throw new RuntimeException(s"Could not build Unity package (see $logFile)");
    }
  }

  def importPackage(projectPath:File, logFile:File, packageFile:File, log:Logger): Unit = {
    val executable = detectUnityExecutable;
    log.info(s"Using $executable");

    val result = List(
      executable.getAbsolutePath(),
      "-batchMode",
      "-quit",
      "-projectPath ",
      projectPath.getAbsolutePath(),
      "-importPackage",
      packageFile.getAbsolutePath()
    ) !;

    if(result != 0) {
      throw new RuntimeException(s"Could not import Unity package $packageFile (see $logFile)");
    }
  }

  private val WindowsPattern = "(.*win.*)".r;
  private val OSXPattern = "(.*mac.*)".r
  private val platformMapping = Map(
    (TargetPlatform.Linux32, ("buildLinux32Player", null)),
    (TargetPlatform.Linux64, ("buildLinux64Player", null)),
    (TargetPlatform.LinuxUniversal, ("buildLinuxUniversalPlayer", null)),
    (TargetPlatform.OSX, ("buildOSXPlayer", "bundle")),
    (TargetPlatform.OSX64, ("buildOSX64Player", "bundle")),
    (TargetPlatform.OSXUniversal, ("buildOSXUniversalPlayer", "bundle")),
    (TargetPlatform.Web, ("buildWebPlayer", null)),
    (TargetPlatform.WebStreamed, ("buildWebPlayerStreamed", null)),
    (TargetPlatform.Windows, ("buildWindowsPlayer", "exe")),
    (TargetPlatform.Windows64, ("buildWindows64Player", "exe"))
  );
}
