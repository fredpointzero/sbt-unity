name := baseDirectory.value.name

version := "0.1"

unitySettings

UnityKeys.unityPackageSourceDirectories in Compile := Seq(s"Assets/${normalizedName.value}_main")