/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
sbtPlugin := true

name := "sbt-unity"

organization := "org.fredericvauchelles"

version := "1.1-SNAPSHOT"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % Test

testOptions in Test += Tests.Argument("-oDSF")