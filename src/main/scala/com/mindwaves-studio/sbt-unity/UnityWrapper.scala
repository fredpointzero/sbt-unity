package com.mindwaves_studio.sbt_unity

import sbt._

/**
 * Created by Fredpointzero on 09/09/2014.
 */
object UnityWrapper {
  import scala.sys.process._

  object BuildTarget extends Enumeration {
    type BuildTarget = Value;
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

  def getBuildTargetCapabilitiesFromOS(osName:String):Seq[BuildTarget.Value] = {
    osName toLowerCase match {
      case WindowsPattern(c) => Seq(BuildTarget.Windows, BuildTarget.Windows64, BuildTarget.Web, BuildTarget.WebStreamed);
      case OSXPattern(c) => Seq(BuildTarget.OSX, BuildTarget.OSX,  BuildTarget.OSXUniversal, BuildTarget.Web, BuildTarget.WebStreamed);
      case _ => Seq(BuildTarget.None);
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

  def buildUnityPlayer(projectPath: File, logFile: File, targetPlatform:BuildTarget.Value, targetDirectory:File, log:Logger) = {
    val buildCapabilities = getBuildTargetCapabilitiesFromOS(System.getProperty("os.name"));
    if (!buildCapabilities.contains(targetPlatform)) {
      throw new IllegalArgumentException(s"Target platform $targetPlatform is not supported on this OS");
    }

    val (buildMethod, ext) = targetPlatform match {
      case BuildTarget.Linux32 => ("buildLinux32Player", null);
      case BuildTarget.Linux64 => ("buildLinux64Player", null);
      case BuildTarget.LinuxUniversal => ("buildLinuxUniversalPlayer", null);
      case BuildTarget.OSX => ("buildOSXPlayer", "bundle");
      case BuildTarget.OSX64 => ("buildOSX64Player", "bundle");
      case BuildTarget.OSXUniversal => ("buildOSXUniversalPlayer", "bundle");
      case BuildTarget.Web => ("buildWebPlayer", null);
      case BuildTarget.WebStreamed => ("buildWebPlayerStreamed", null);
      case BuildTarget.Windows => ("buildWindowsPlayer", "exe");
      case BuildTarget.Windows64 => ("buildWindows64Player", "exe");
      case _ => throw new IllegalArgumentException(s"Unmanaged build target: $targetPlatform");
    }

    val parentDir = file(targetDirectory.getParent());
    if (!parentDir.exists()) {
      parentDir.mkdirs();
    }

    var targetPath = targetDirectory.getAbsolutePath();
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

  def buildUnityPackage(projectPath: File, targetParentDirectory:File, definitions:Seq[Tuple2[String, Seq[String]]], log:Logger) = {

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

    if (!targetParentDirectory.exists()) {
      targetParentDirectory.mkdirs();
    }

    var failedPackages:List[String] = List();
    for((alias:String, paths) <- definitions) {
      val logFile = targetParentDirectory / s"${alias}.log";
      val result = (commonCmd ++
        (paths :+ (targetParentDirectory / s"${alias}.unitypackage").toString()) ++
        Seq("-logFile", logFile.toString())) !;

      if(result != 0) {
        failedPackages = failedPackages :+ alias;
      }
    }
    if (failedPackages.size > 0) {
      throw new RuntimeException(s"Could not build following Unity packages: (${failedPackages.mkString(",")}) (see in $targetParentDirectory)");
    }
  }

  private val WindowsPattern = "(.*win.*)".r;
  private val OSXPattern = "(.*mac.*)".r;
}
