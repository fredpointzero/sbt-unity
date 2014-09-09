package com.mindwaves_studio.sbt_unity

import java.nio.file.Files

import sbt._
import sbt.Keys._

/**
 * Created by fredericvauchelles on 07/09/2014.
 */
object UnityPlugin extends sbt.Plugin{
  import UnityKeys._

  object UnityKeys {
    val unityEditorExecutable = SettingKey[File]("unity-editor-executable", "Path to the Unity editor executable to use")
    val unitySource = SettingKey[File]("unity-source", "Default Unity source directory")
    val generateWorkspace = TaskKey[File]("generate-workspace", "Generate a Unity workspace")
  }

  private def generateWorkspaceTaskById(workspaceId:String, c:Configuration) = (unitySource in c, target in c, normalizedName, streams) map { (sourceDir, targetDir, normName, s) => {
    val unityWorkspaceDirectory = targetDir / s"unity${workspaceId}Workspace";
    val assetDirectory = unityWorkspaceDirectory / "Assets";
    // Make directories if necessary
    if (!assetDirectory.exists()) {
      assetDirectory.mkdirs();
    }

    val linkedDirectory = assetDirectory / normName;
    // Replace the target and create the symlink
    if (linkedDirectory.exists() && !Files.isSymbolicLink(linkedDirectory toPath)) {
      s.log.info(s"Replacing directory $linkedDirectory by a symlink");
      linkedDirectory.delete();
    }
    if (!linkedDirectory.exists()) {
      Files.createSymbolicLink(linkedDirectory toPath, sourceDir toPath);
    }
    else {
      s.log.info(s"Skipping $linkedDirectory as it already exists");
    }

    // Create the Unity project
    UnityWrapper.createUnityProjectAt(unityWorkspaceDirectory, targetDir / s"unity${workspaceId}WorkspaceCreation.log");

    linkedDirectory;
  }}

  def unitySettings: Seq[Setting[_]] =
    unitySettingsIn(Compile) ++ unitySettingsIn(Test)

  private def unitySettingsIn(c: Configuration): Seq[Setting[_]] =
    inConfig(c)(unitySettings0 ++ Seq(
      generateWorkspace in c <<= generateWorkspaceTaskById(s"Build$c", c)
    ))

  private def unitySettings0: Seq[Setting[_]] = Seq(
    unityEditorExecutable := UnityWrapper.detectUnityExecutable,
    unitySource := sourceDirectory.value / "runtime_assets"
  );
}
