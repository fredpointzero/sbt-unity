name := baseDirectory.value.name

version := "0.1"

unityPlayerSettings

UnityKeys.unityIntegrationTestScenes in Test := Seq("ExampleIntegrationTests")

UnityKeys.unityUnitTestFilters in Test := Seq("UnityTest.SampleTests.PassingTest")