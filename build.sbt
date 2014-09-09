sbtPlugin := true

name := "sbt-unity"

organization := "com.mindwaves-studio"

version := "1.0-SNAPSHOT"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"

testOptions in Test += Tests.Argument("-oDSF")