name := baseDirectory.value.name

version := "0.1"

unitySettings

mappings := Seq((file(""), s"Assets/${normalizedName.value}_main"))