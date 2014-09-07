ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxHeapSize=512M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false