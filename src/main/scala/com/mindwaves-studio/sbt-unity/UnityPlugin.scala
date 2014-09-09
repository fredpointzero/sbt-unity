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
    val unityEditorExecutable = SettingKey[File]("unity-editor-executable", "Path to the Unity editor executable to use")
    val unitySourceDirectories = SettingKey[Seq[(String, File)]]("unity-source-directories", "Default Unity source directories")
    val unityBuildTarget = SettingKey[UnityWrapper.UnityBuildTarget.Value]("unity-build-target", "Target platform for the build")
    val generateWorkspace = TaskKey[File]("generate-workspace", "Generate a Unity workspace")
  }

  private def generateWorkspaceTaskById(workspaceId:String, c:Configuration) = (unitySourceDirectories in c, target in c, normalizedName, streams) map {
    (sourceAliases, targetDir, normName, s) => {

    val unityWorkspaceDirectory = targetDir / s"unity${workspaceId}${c}Workspace";
    val assetDirectory = unityWorkspaceDirectory / "Assets";
    // Make directories if necessary
    if (!assetDirectory.exists()) {
      assetDirectory.mkdirs();
    }

    // Create the Unity project
    if (!(unityWorkspaceDirectory / "Library").exists()) {
      UnityWrapper.createUnityProjectAt(unityWorkspaceDirectory, targetDir / s"unity${workspaceId}${c}WorkspaceCreation.log");
    }

    for ((sourceAlias, sourceDir:File) <- sourceAliases) {
      val linkedDirectory = assetDirectory / s"${normName}_$sourceAlias";
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

    unityWorkspaceDirectory;
  }}

  def unitySettings: Seq[Setting[_]] =
    inConfig(Compile)(unitySettings0 ++ Seq(
      unitySourceDirectories in Compile := Seq(
        ("main", (sourceDirectory in Compile).value / "runtime_resources")
      ),
      generateWorkspace in Compile <<= generateWorkspaceTaskById("Build", Compile),
      compile in Compile <<= (generateWorkspace in Compile, unityBuildTarget in Compile, target) map { (generatedWorkspaceDir, buildTarget, targetDir) => {
        val targetDirectory = targetDir / s"build_${buildTarget}_main";
        if(!targetDirectory.exists()) {
          targetDirectory.mkdirs();
        }
        UnityWrapper.buildUnityPlayer(generatedWorkspaceDir, targetDir / s"build_${buildTarget}_main.log", buildTarget, targetDirectory);
        Analysis.Empty
      }}
    )) ++
    inConfig(Test)(unitySettings0 ++ Seq(
      unitySourceDirectories in Test := Seq(
        ("main", (sourceDirectory in Compile).value / "runtime_resources"),
        ("test", (sourceDirectory in Test).value / "runtime_resources")
      ),
      generateWorkspace in Test <<= generateWorkspaceTaskById("Build", Test)
    ))

  private def unitySettings0: Seq[Setting[_]] = Seq(
    unityEditorExecutable := UnityWrapper.detectUnityExecutable,
    unityBuildTarget := UnityWrapper.getBuildTargetCapabilitiesFromOS(System.getProperty("os.name"))(0)
  );
}
