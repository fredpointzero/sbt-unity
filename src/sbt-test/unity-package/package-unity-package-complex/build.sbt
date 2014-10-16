/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
name := baseDirectory.value.name

version := "0.1"

unityPackageSettings

testSettings

mappings in (Compile, packageBin) := Seq(
  (file(""), s"Assets/${normalizedName.value}"),
  (file(""), "ProjectSettings")
)

// Set sbt-unity-package version
UnityKeys.unityPackageToolsVersion := System.getProperty("package.version")