package com.mindwaves_studio.sbt_unity

import sbt._

/**
 * Created by Fredpointzero on 09/09/2014.
 */
object UnityWrapper {

  private val WindowsPattern = ".*win.*".r;
  private val OSXPattern = ".*mac.*".r;

  def detectUnityExecutable = {
    val systemUnityExecutable = System.getProperty("UNITY_EDITOR_PATH");
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
      case WindowsPattern(c) => file("");
      case OSXPattern(c) => file("");
      case _ => throw new RuntimeException(s"This OS ($osName) is not managed by Unity Editor");
    }
  }
}