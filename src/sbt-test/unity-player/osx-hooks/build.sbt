/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
name := baseDirectory.value.name

version := "0.1"

unityPlayerSettings

// Set sbt-unity-package version
UnityKeys.unityPackageToolsVersion := System.getProperty("package.version")

// Skip tests here
UnityKeys.unityIntegrationTestSkip := true

UnityKeys.unityUnitTestSkip := true

UnityKeys.unityHooks := Seq(
  (Hook.PreCompile, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "precompile.txt").toString), true),
  (Hook.PostCompile, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "postcompile.txt").toString), true),
  (Hook.PreTest, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "pretest.txt").toString), true),
  (Hook.PostTest, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "posttest.txt").toString), true),
  (Hook.PrePackage, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "prepackage.txt").toString), true)
)