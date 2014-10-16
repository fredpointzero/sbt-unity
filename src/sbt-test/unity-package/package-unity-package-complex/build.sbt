/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
name := baseDirectory.value.name

version := "0.1"

unityPackageSettings

testSettings

mappings.in(Compile, packageBin) := Seq((file(""), "ProjectSettings"), (file(""), s"Assets/${normalizedName.value}"))