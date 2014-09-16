name := baseDirectory.value.name

version := "0.1"

unityPackageSettings

mappings.in(Compile, packageBin) := Seq((file(""), s"Assets/${normalizedName.value}_main"))