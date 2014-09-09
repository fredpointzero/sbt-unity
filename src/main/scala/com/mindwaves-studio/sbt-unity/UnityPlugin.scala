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

  def unitySettings: Seq[Setting[_]] = Seq(
    unityEditorExecutable := UnityWrapper.detectUnityExecutable,
    unitySource in Compile := (sourceDirectory in Compile).value / "runtime_assets",
    generateWorkspace in Compile <<= (unitySource in Compile, target in Compile, normalizedName, streams) map { (sourceDir, targetDir, normName, s) => {
      val assetDirectory = targetDir / "unityBuildWorkspace/Assets";
      if (!assetDirectory.exists()) {
        assetDirectory.mkdirs();
      }
      val linkedDirectory = assetDirectory / normName;
      if (!linkedDirectory.exists()) {
        Files.createSymbolicLink(linkedDirectory toPath, sourceDir toPath);
      }
      else {
        s.log.info(s"Skipping $linkedDirectory as it already exists");
      }
      linkedDirectory;
    }}
  )
}
