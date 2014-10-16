/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq(
    "-Xmx1024M",
    "-XX:MaxHeapSize=512M",
    "-Dplugin.version=" + version.value,
    "-Dpackage.version=" + version.value,
    "-Xdebug",
    "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:5006")
}

scriptedBufferLog := false