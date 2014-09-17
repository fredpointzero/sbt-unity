name := baseDirectory.value.name

version := "0.1"

unityPackageSettings

testSettings

mappings.in(Compile, packageBin) := Seq((file(""), "ProjectSettings"), (file(""), s"Assets/${normalizedName.value}"))