/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
name := baseDirectory.value.name

version := "0.1"

unityPlayerSettings

resolvers += "Local Maven Repository" at "file:///" + baseDirectory.value + "/.m2/repository"

libraryDependencies += "org.fredericvauchelles" % "dummy_test" % "1.0" artifacts Artifact ("dummy_test", "unitypackage", "unitypackage")

// Set sbt-unity-package version
UnityKeys.unityPackageToolsVersion := System.getProperty("package.version")