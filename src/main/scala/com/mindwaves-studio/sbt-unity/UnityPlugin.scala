package com.mindwaves_studio.sbt_unity

import java.nio.file.Files

import sbt._
import sbt.Keys._
import sbt.inc.Analysis

/**
 * Created by fredericvauchelles on 07/09/2014.
 */
object UnityPlugin extends sbt.Plugin{
  import UnityKeys._

  object UnityKeys {
    // Paths
    val unitySource = SettingKey[Seq[File]]("unity-source", "Default Unity source directories")
    val workspaceDirectory = SettingKey[File]("workspace-directory", "Directory of the Unity workspace")

    // Tasks
    val generateWorkspace = TaskKey[File]("generate-workspace", "Generate a Unity workspace")

    // Unity Options
    val crossPlatform = SettingKey[UnityWrapper.TargetPlatform.Value]("cross-platform", "Target platform for the build")
    val unityEditorExecutable = SettingKey[File]("unity-editor-executable", "Path to the Unity editor executable to use")
  }

  def unityPlayerSettings: Seq[Setting[_]] = unityCommonSettings ++ Seq(
    // Cross building
    crossPlatform := UnityWrapper.TargetPlatform.None
  ) ++ inConfig(Compile)(Seq(
    crossTarget := target.value / crossPlatform.value.toString(),

    compile := compileTask.value,
    products <<= productsTask,
    artifact := artifactSetting.value,
    run := runTask.value
  )) ++ inConfig(Test)(Seq(
    crossTarget := target.value / crossPlatform.value.toString(),

    compile := compileTask.value,
    products <<= productsTask,
    artifact := artifactSetting.value,
    run := runTask.value
  ))

  def unityPackageSettings: Seq[Setting[_]] = unityCommonSettings ++ Seq(
    // Tasks
    products <<= Def.task { Nil },
    compile := {
      val x1 = generateWorkspace.value;
      Analysis.Empty;
    },
    artifact := { Artifact.apply(name.value, "unitypackage", "unitypackage", s"${configuration}"); },
    packageBin in Compile := {
      val x1 = generateWorkspace.value;
      UnityWrapper.buildUnityPackage(workspaceDirectory.value, artifactPath.value, file(artifactPath.value.toString() + ".log"), (mappings.in(Compile, packageBin)).value map { a => a._2 }, streams.value.log);
      artifactPath.value;
    },
    skip in run := true
  )

  private def unityCommonSettings: Seq[Setting[_]] = Seq(
    // Unity options
    unityEditorExecutable := UnityWrapper.detectUnityExecutable,
    artifactName := {
      (scalaVersion:ScalaVersion, module:ModuleID, artifact:Artifact) => {
        import artifact._
        val classifierStr = classifier match { case None => ""; case Some(c) => "-" + c }
        artifact.name + "_" + crossPlatform.value + "-" + module.revision + classifierStr + "." + artifact.extension
      }
    },
  mappings.in(Compile, packageBin) <<= (mappings.in(Compile, packageBin), streams) map { (f, s) =>
    s.log.warn(s"Mapping with ${f.size} file")
    for((file, path) <- f) s.log.warn(s"$file -> $path");
    f
  }
  ) ++ inConfig(Compile)(Seq(
    unitySource := Seq(sourceDirectory.value / SOURCES_FOLDER_NAME, sourceDirectory.value / SETTINGS_FOLDER_NAME),
    unmanagedSourceDirectories := unitySource.value,

    // Workspace
    workspaceDirectory := target.value / "workspace",
    generateWorkspace := generateWorkspaceTask.value
  )) ++ inConfig(Test)(Seq(
    unitySource := Seq(
      sourceDirectory.value / SOURCES_FOLDER_NAME,
      (sourceDirectory in Compile).value / SOURCES_FOLDER_NAME,
      sourceDirectory.value / SETTINGS_FOLDER_NAME
    ),
    unmanagedSourceDirectories := unitySource.value,

    // Workspace
    workspaceDirectory := target.value / "test-workspace",
    generateWorkspace := generateWorkspaceTask.value
  ))

  def extractSourceDirectoryContext(path:File):String =
    extractAnyDirectoryContext(path, SOURCES_FOLDER_NAME);

  def extractSettingsDirectoryContext(path:File):String =
    extractAnyDirectoryContext(path, SETTINGS_FOLDER_NAME);

  private val SOURCES_FOLDER_NAME = "runtime_resources";
  private val SETTINGS_FOLDER_NAME = "unity_settings";
  private val ANY_PATH_PATTERN = "([^\\\\/]*)(?:\\\\|/)([^\\\\/]*)$".r;

  private def extractAnyDirectoryContext(path:File, folderName:String):String = {
    val matches = ANY_PATH_PATTERN findAllIn(path toString);
    if (matches.hasNext && matches.group(2) == folderName) {
      val context = matches.group(1);
      return context;
    }
    else {
      return null;
    }
  }

  private def artifactSetting = Def.setting { Artifact.apply(name.value, UnityWrapper.extensionForPlatform(crossPlatform.value), "jar", s"${configuration}-$crossPlatform"); }

  private def productsTask = Def.task {
    val x1 = compile.value;
    Seq(crossTarget.value)
  }

  private def runTask = Def.task {
    val x1 = compile.value;
    val executable = crossTarget.value / (normalizedName.value + "." + UnityWrapper.extensionForPlatform(crossPlatform.value));
    executable.toString() !;
  }

  private def compileTask = Def.task {
    if(!crossTarget.value.exists()) {
      crossTarget.value.mkdirs();
    }
    val x1 = generateWorkspace.value;
    UnityWrapper.buildUnityPlayer(workspaceDirectory.value, file(crossTarget.value.toString() + ".log"), crossPlatform.value, crossTarget.value / normalizedName.value, streams.value.log);
    Analysis.Empty;
  }

  private def generateWorkspaceTask = Def.task {
    val assetDirectory = workspaceDirectory.value / "Assets";
    // Make directories if necessary
    if (!assetDirectory.exists()) {
      assetDirectory.mkdirs();
    }

    // Create the Unity project
    if (!(workspaceDirectory.value / "Library").exists()) {
      UnityWrapper.createUnityProjectAt(workspaceDirectory.value, target.value / s"${workspaceDirectory.value}.log", streams.value.log);
    }

    for (sourceDir <- unitySource.value) {
      val sourcesContext = extractSourceDirectoryContext(sourceDir);
      if (sourcesContext != null) {
        val suffix = if (sourcesContext == "main") "" else s"_${sourcesContext}";
        val linkedDirectory = assetDirectory / s"${normalizedName.value}$suffix";
        // Replace the target and create the symlink
        if (linkedDirectory.exists() && !Files.isSymbolicLink(linkedDirectory toPath)) {
          streams.value.log.info(s"Replacing directory $linkedDirectory by a symlink");
          linkedDirectory.delete();
        }
        if (!linkedDirectory.exists()) {
          if(sourceDir.exists()) {
            Files.createSymbolicLink(linkedDirectory toPath, sourceDir toPath);
          }
          else {
            streams.value.log.info(s"Skipping $linkedDirectory because $sourceDir does not exists");
          }
        }
        else {
          streams.value.log.info(s"Skipping $linkedDirectory as it already exists");
        }
      }

      val settingsContext = extractSettingsDirectoryContext(sourceDir);
      if (settingsContext != null && sourceDir.exists()) {
        for(settingFile <- sourceDir.listFiles("*.asset")) {
          val targetLink = workspaceDirectory.value / "ProjectSettings" / settingFile.name;
          if(targetLink.exists()) {
            streams.value.log.info(s"Deleting existing setting: $targetLink");
            targetLink.delete();
          }
          Files.createSymbolicLink(targetLink toPath, settingFile toPath);
        }
      }
    }

    val unmanagedLibFiles = unmanagedBase.value.listFiles;
    if (unmanagedLibFiles != null)  {
      for (packageFile:File <- unmanagedLibFiles.filter(_.ext == "unitypackage")) {
        UnityWrapper.importPackage(workspaceDirectory.value, workspaceDirectory.value / s"import-${packageFile.name}.log", packageFile, streams.value.log);
      }
    }

    workspaceDirectory.value;
  }
}
