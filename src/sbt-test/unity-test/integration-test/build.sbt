/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
name := baseDirectory.value.name

version := "0.1"

unityPlayerSettings

UnityKeys.unityIntegrationTestScenes in Test := Seq("ExampleIntegrationTests")

UnityKeys.unityUnitTestFilters in Test := Seq("UnityTest.SampleTests.PassingTest")

// Set sbt-unity-package version
UnityKeys.unityPackageToolsVersion := System.getProperty("package.version")