package com.mindwaves_studio.sbt_unity

import sbt._
import sbt.Keys._

/**
 * Created by fredericvauchelles on 07/09/2014.
 */
object UnityPlugin extends sbt.Plugin{
  import UnityKeys._

  object UnityKeys {
    val generateWorkspace = TaskKey[File]("generateWorkspace", "Generate a Unity workspace")
  }

  def unitySettings = Seq(
    generateWorkspace <<= sourceDirectory in generateWorkspace map {(dir) => {println(dir); dir}}
  )
}
