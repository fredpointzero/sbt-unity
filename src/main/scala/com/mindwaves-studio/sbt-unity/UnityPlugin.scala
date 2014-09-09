package com.mindwaves_studio.sbt_unity

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
    val generateBuildWorkspace = TaskKey[File]("generate-build-workspace", "Generate a Unity build workspace")
  }

  def unitySettings: Seq[Setting[_]] = Seq(
    unityEditorExecutable := UnityWrapper.detectUnityExecutable,
    unitySource := (sourceDirectory in Compile).value / "runtime_assets",
    generateBuildWorkspace <<= (unitySource in Compile) map { (dir) => {println(dir); dir} }
  )
}
