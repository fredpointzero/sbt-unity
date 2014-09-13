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

  object Pipeline extends Enumeration {
    type Pipeline = Value;
    val UnityPlayer, UnityPackage, None = Value;
  }

  object UnityKeys {
    // Paths
    val unityPackageSourceDirectories = SettingKey[Seq[String]]("unity-package-source-directories", "Define the Unity relative directories to export as Unity package")
    val unitySource = SettingKey[Seq[File]]("unity-source", "Default Unity source directories")
    val workspaceDirectory = SettingKey[File]("workspace-directory", "Directory of the Unity workspace")

    // Tasks
    val generateWorkspace = TaskKey[File]("generate-workspace", "Generate a Unity workspace")
    val importUnmanagedUnityPackages = TaskKey[Unit]("import-unmanaged-unity-package", "Import unmanaged Unity packages")

    // Unity Options
    val crossPlatform = SettingKey[UnityWrapper.TargetPlatform.Value]("cross-platform", "Target platform for the build")
    val unityEditorExecutable = SettingKey[File]("unity-editor-executable", "Path to the Unity editor executable to use")
    val unityPipeline = SettingKey[Pipeline.Value]("unity-pipeline", "Pipeline to use")
  }

  def unitySettings: Seq[Setting[_]] = unitySettings0 ++
    inConfig(Test)(Seq(
      unmanagedSourceDirectories += (unmanagedSourceDirectories in Compile).value / SOURCES_FOLDER_NAME,
      unitySource += (sourceDirectory in Compile).value / SOURCES_FOLDER_NAME
    ))

  private def unitySettings0: Seq[Setting[_]] = Seq(
    // Paths
    crossTarget := target.value / crossPlatform.value.toString(),
    unitySource := Seq(sourceDirectory.value / SOURCES_FOLDER_NAME, sourceDirectory.value / SETTINGS_FOLDER_NAME),
    unmanagedSourceDirectories ++= unitySource.value,
    unityPackageSourceDirectories := Seq(),

    // Workspace options
    workspaceDirectory := target.value / (Defaults.prefix(configuration.value.name) + "workspace"),
    importUnmanagedUnityPackages := importUnmanagedUnityPackageTask,
    generateWorkspace := generateWorkspaceTask,

    // Unity Options
    unityEditorExecutable := UnityWrapper.detectUnityExecutable,
    unityPipeline := Pipeline.None,

    // Build Player Options
    crossPlatform := UnityWrapper.TargetPlatform.None,
    products <<= Def.task {
      unityPipeline match {
        case Pipeline.UnityPlayer => crossTarget.value :: Nil;
        case Pipeline.UnityPackage => Nil;
        case _ => throw new RuntimeException(s"Unmanaged pipeline: $unityPipeline");
      }
    },

    // Standard task
    compile := compileTask,
    artifact := {
      unityPipeline match {
        case Pipeline.UnityPlayer => Artifact.apply(name.value, UnityWrapper.extensionForPlatform(crossPlatform.value), "jar", s"${configuration}-$crossPlatform");
        case Pipeline.UnityPackage => Artifact.apply(name.value, "unitypackage", "unitypackage", s"${configuration}");
        case _ => throw new RuntimeException(s"Unmanaged pipeline: $unityPipeline");
      }
    },
    artifactPath := {
      target.value / artifactName.value(ScalaVersion(scalaVersion.value, scalaBinaryVersion.value), moduleID.value, artifact.value);
    },
    artifactName := { (scalaVersion, moduleId, artifact) => {
      unityPipeline match {
        case Pipeline.UnityPlayer | Pipeline.UnityPackage => artifact toString;
        case _ => throw new RuntimeException(s"Unmanaged pipeline $unityPipeline");
      }
    } },
    packageBin :=  {
      //TODO: create a unitypackage or standard package for unity player
      unityPipeline match {
        case Pipeline.UnityPlayer => packageBin.value;
        case Pipeline.UnityPackage => {
          val x1 = generateWorkspace.value;
          UnityWrapper.buildUnityPackage(workspaceDirectory.value, artifactPath.value, file(artifactPath.value.toString() + ".log"), mappings.value map { a => a._2 }, streams.value.log);
          artifactPath.value;
        }
        case _ => throw new RuntimeException(s"Unmanaged pipeline $unityPipeline");
      }
      packageBin.value
    },
    run := {
      unityPipeline match {
        case Pipeline.UnityPlayer => {
          val x1 = compile.value;
          val executable = crossTarget.value / (normalizedName.value + UnityWrapper.extensionForPlatform(crossPlatform.value));
          executable.toString() !;
        };
        case Pipeline.UnityPackage => throw new RuntimeException("Cannot run a Unity package");
        case _ => throw new RuntimeException(s"Unmanaged pipeline $unityPipeline");
      }
    },
    sbt.Keys.test := {
      unityPipeline.value match {
        case Pipeline.UnityPlayer | Pipeline.UnityPackage => {
          val x1 = compile.value;
          streams.value.log.error("test are not implemented");
        }
        case _ => throw new RuntimeException(s"Unknown pipeline: $unityPipeline")
      }
    }
  );

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

  private def importUnmanagedUnityPackageTask = {
      val x1 = generateWorkspace.value;
      for (packageFile:File <- unmanagedBase.value.filter(f => f.ext == "unitypackage")) {
        UnityWrapper.importPackage(workspaceDirectory.value, workspaceDirectory.value / s"import-${packageFile.name}.log", packageFile, streams.value.log);
      }
    }

  private def compileTask = {
    unityPipeline.value match {
      case Pipeline.UnityPlayer => {
        if(!crossTarget.value.exists()) {
          crossTarget.value.mkdirs();
        }
        val x1 = generateWorkspace.value;
        UnityWrapper.buildUnityPlayer(workspaceDirectory.value, file(crossTarget.value.toString() + ".log"), crossPlatform.value, crossTarget.value / normalizedName.value, streams.value.log);
      }
      case Pipeline.UnityPackage => {
      }
      case _ => throw new RuntimeException(s"Unknown pipeline: $unityPipeline")
    }

    Analysis.Empty
    }

  private def generateWorkspaceTask = {
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
        val linkedDirectory = assetDirectory / s"${normalizedName.value}_${sourcesContext}";
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

    val x1 = importUnmanagedUnityPackages.value;

    workspaceDirectory.value;
  }
}
