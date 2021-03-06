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

UnityKeys.unityHooks in Compile := Seq(
  (Hook.PreCompile, "FV.TouchFile.Touch", Seq(((sbt.Keys.crossTarget in Compile).value / "precompile-compile.txt").toString), true),
  (Hook.PostCompile, "FV.TouchFile.Touch", Seq(((sbt.Keys.crossTarget in Compile).value / "postcompile-compile.txt").toString), true),
  (Hook.PrePackage, "FV.TouchFile.Touch", Seq(((sbt.Keys.crossTarget in Compile).value / "prepackage-compile.txt").toString), true)
)

UnityKeys.unityHooks in Test := Seq(
  (Hook.PreCompile, "FV.TouchFile.Touch", Seq(((sbt.Keys.crossTarget in Test).value / "precompile-test.txt").toString), true),
  (Hook.PostCompile, "FV.TouchFile.Touch", Seq(((sbt.Keys.crossTarget in Test).value / "postcompile-test.txt").toString), true),
  (Hook.PreTest, "FV.TouchFile.Touch", Seq(((sbt.Keys.crossTarget in Test).value / "pretest-test.txt").toString), true),
  (Hook.PostTest, "FV.TouchFile.Touch", Seq(((sbt.Keys.crossTarget in Test).value / "posttest-test.txt").toString), true)
)