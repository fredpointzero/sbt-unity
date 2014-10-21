/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
name := baseDirectory.value.name

version := "0.1"

unityPackageSettings

// Set sbt-unity-package version
UnityKeys.unityPackageToolsVersion := System.getProperty("package.version")

// Skip tests here
UnityKeys.unityIntegrationTestSkip := true

UnityKeys.unityUnitTestSkip := true

UnityKeys.unityHooks in Compile := Seq(
  (Hook.PreCompile, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "precompile-compile.txt").toString), true),
  (Hook.PostCompile, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "postcompile-compile.txt").toString), true),
  (Hook.PreTest, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "pretest-compile.txt").toString), true),
  (Hook.PostTest, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "posttest-compile.txt").toString), true),
  (Hook.PrePackage, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "prepackage-compile.txt").toString), true)
)

UnityKeys.unityHooks in Test := Seq(
  (Hook.PreCompile, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "precompile-test.txt").toString), true),
  (Hook.PostCompile, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "postcompile-test.txt").toString), true),
  (Hook.PreTest, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "pretest-test.txt").toString), true),
  (Hook.PostTest, "FV.TouchFile.Touch", Seq((sbt.Keys.crossTarget.value / "posttest-test.txt").toString), true)
)