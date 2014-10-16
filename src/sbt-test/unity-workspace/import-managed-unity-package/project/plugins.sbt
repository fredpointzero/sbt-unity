/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
logLevel := sbt.Level.Warn

{
  val pluginVersion = System.getProperty("plugin.version")
  if(pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
                                 |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  else
    addSbtPlugin("com.mindwaves-studio" % "sbt-unity" % pluginVersion)
}