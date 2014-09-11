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
    val buildTarget = SettingKey[UnityWrapper.BuildTarget.Value]("build-target", "Target platform for the build")
    val generateWorkspace = TaskKey[File]("generate-workspace", "Generate a Unity workspace")
    val importUnityPackage = TaskKey[Unit]("import-unity-package", "Import a unity package in the project")
    val unityEditorExecutable = SettingKey[File]("unity-editor-executable", "Path to the Unity editor executable to use")
    val unityPackageSourceDirectories = SettingKey[Seq[String]]("unity-package-source-directories", "Define the Unity relative directories to export as Unity package")
    val unitySource = SettingKey[Seq[File]]("unity-source", "Default Unity source directories")
  }

  def unitySettings: Seq[Setting[_]] =
    inConfig(Compile)(unitySettings0 ++ Seq(
      generateWorkspace in Compile <<= generateWorkspaceTaskById("Build", Compile),
      compile in Compile <<= compileTaskIn(Compile),
      importUnityPackage in Compile <<= importPackageTaskIn(Compile)
    )) ++
    inConfig(Test)(unitySettings0 ++ Seq(
      (sourceDirectories in Test) += (sourceDirectory in Compile).value / SOURCES_FOLDER_NAME,
      (unitySource in Test) += (sourceDirectory in Compile).value / SOURCES_FOLDER_NAME,
      generateWorkspace in Test <<= generateWorkspaceTaskById("Build", Test),
      compile in Test <<= compileTaskIn(Test),
      importUnityPackage in Test <<= importPackageTaskIn(Test)
    ))

  private def unitySettings0: Seq[Setting[_]] = Seq(
    unityEditorExecutable := UnityWrapper.detectUnityExecutable,
    buildTarget := UnityWrapper.BuildTarget.None,
    unityPackageSourceDirectories := Seq(),
    unitySource := Seq(sourceDirectory.value / SOURCES_FOLDER_NAME, sourceDirectory.value / SETTINGS_FOLDER_NAME),
    sourceDirectories ++= Seq(sourceDirectory.value / SOURCES_FOLDER_NAME, sourceDirectory.value / SETTINGS_FOLDER_NAME)
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

  private def importPackageTaskIn(c:Configuration) =
    (generateWorkspace in c, resources in c, target in c, streams) map {
      (workspaceDir, resourceFiles, targetDir, s) => {
        for (packageFile <- resourceFiles.filter(f => f.ext == "unitypackage")) {
          UnityWrapper.importPackage(workspaceDir, targetDir / s"import-${packageFile.name}.log", packageFile, s.log);
        }
      }
    }

  private def compileTaskIn(c:Configuration) =
    (generateWorkspace in c, buildTarget in c, unityPackageSourceDirectories in c, target, normalizedName, streams) map {
    (generatedWorkspaceDir, buildTarget, packageDirectories, targetDir, normName, s) => {
      // Build player
      if (buildTarget != UnityWrapper.BuildTarget.None) {
        val targetDirectory = targetDir / s"${buildTarget}/${normName}";
        if(!targetDirectory.exists()) {
          targetDirectory.mkdirs();
        }
        UnityWrapper.buildUnityPlayer(generatedWorkspaceDir, targetDir / s"build_${buildTarget}.log", buildTarget, targetDirectory, s.log);
      }
      else {
        s.log.info("Skipping player build");
      }

      // Build Unity Packages
      if (packageDirectories.size > 0) {
        UnityWrapper.buildUnityPackage(generatedWorkspaceDir, targetDir / s"${normName}.unitypackage", targetDir / s"${normName}.unitypackage.log", packageDirectories, s.log);
      }
      else {
        s.log.info("Skipping Unity package build");
      }

      Analysis.Empty
    }}

  private def generateWorkspaceTaskById(workspaceId:String, c:Configuration) = (unitySource in c, target in c, normalizedName, streams) map {
  (sourceDirs, targetDir, normName, s) => {
    val unityWorkspaceDirectory = targetDir / s"unity${workspaceId}${c}Workspace";
    val assetDirectory = unityWorkspaceDirectory / "Assets";
    // Make directories if necessary
    if (!assetDirectory.exists()) {
      assetDirectory.mkdirs();
    }

    // Create the Unity project
    if (!(unityWorkspaceDirectory / "Library").exists()) {
      UnityWrapper.createUnityProjectAt(unityWorkspaceDirectory, targetDir / s"unity${workspaceId}${c}WorkspaceCreation.log", s.log);
    }

    for (sourceDir <- sourceDirs) {
      val sourcesContext = extractSourceDirectoryContext(sourceDir);
      if (sourcesContext != null) {
        val linkedDirectory = assetDirectory / s"${normName}_${sourcesContext}";
        // Replace the target and create the symlink
        if (linkedDirectory.exists() && !Files.isSymbolicLink(linkedDirectory toPath)) {
          s.log.info(s"Replacing directory $linkedDirectory by a symlink");
          linkedDirectory.delete();
        }
        if (!linkedDirectory.exists()) {
          if(sourceDir.exists()) {
            Files.createSymbolicLink(linkedDirectory toPath, sourceDir toPath);
          }
          else {
            s.log.info(s"Skipping $linkedDirectory because $sourceDir does not exists");
          }
        }
        else {
          s.log.info(s"Skipping $linkedDirectory as it already exists");
        }
      }

      val settingsContext = extractSettingsDirectoryContext(sourceDir);
      if (settingsContext != null && sourceDir.exists()) {
        for(settingFile <- sourceDir.listFiles("*.asset")) {
          val targetLink = unityWorkspaceDirectory / "ProjectSettings" / settingFile.name;
          if(targetLink.exists()) {
            s.log.info(s"Deleting existing setting: $targetLink");
            targetLink.delete();
          }
          Files.createSymbolicLink(targetLink toPath, settingFile toPath);
        }
      }
    }

    unityWorkspaceDirectory;
  }}
}
