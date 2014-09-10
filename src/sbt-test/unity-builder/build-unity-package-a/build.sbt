name := baseDirectory.value.name

version := "0.1"

unitySettings

UnityKeys.unityPackageDefinitions in Compile := Seq(
  (name.value, Seq("Assets/Test.cs"))
)