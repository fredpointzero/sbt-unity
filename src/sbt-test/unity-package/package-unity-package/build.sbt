/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
name := baseDirectory.value.name

version := "0.1"

testSettings

unityPackageSettings

mappings.in(Compile, packageBin) := Seq((file(""), s"Assets/${normalizedName.value}"))