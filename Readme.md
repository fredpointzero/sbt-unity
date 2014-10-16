# SBT Unity Plugin

## Overview

The SBT Unity plugin is designed to provide an easy way to include your Unity3D project in continuous integration solutions and to ease team collaboration.

It is based on the SBT build pipeline to build, package, run, run tests (unit and integrations tests).

It is based on the SBT dependency management to resolve dependencies (Ivy2 and maven compatible repositories)

## How to use the plugin

1. Make it available to your SBT dependency resolver (choose the most appropriate solution)
    - Publish locally the plugin: `sbt publish-local`
    - Publish the artifact in you preferred ivy2 or maven repository and add the  corresponding resolver [(see SBT Reference)](http://www.scala-sbt.org/0.13/docs/Resolvers.html)
1. Perform the same operation on the **sbt-unity-package** project
1. Add the plugin in the project folder (replace **VERSION**, by the version you published):
_plugin.sbt_
```sbt
addSbtPlugin("org.fredericvauchelles" % "sbt-unity" % "VERSION")
```
1. Add a unity setting to your project to enable the plugin (choose the most appropriate):
 - _build.sbt_
```sbt
// To build a player
unityPlayerSettings
```
 - _build.sbt_
```sbt
// To build a unitypackage
unityPackageSettings
```
1. Use SBT to build what you want !
    - For both build pipelines:
        - Run tests: `sbt test`
        - Generate the main workspace: `sbt generateWorkspace`
        - Generate the test workspace: `sbt test:generateWorkspace`
    - For Player pipeline:
        - Set the target platform: `set UnityKeys.crossPlatform := org.fredericvauchelles.sbt_unity.UnityWrapper.TargetPlatform.Windows`
        - Build a player: `sbt compile`
        - Package a player (produce a zip): `sbt package`
    - For Package pipeline:
        - Create the unitypackage: `sbt package`
1. To use Unity3D to edit your project:
    * Generate the proper workspace (I tend to use the test workspace to develop my project)
    * Open it in Unity3D (either **target/workspace**, or **target/test-workspace**)
    * **Go to Edit > Project Settings > Editor, and set the _Version Control_ to _Visible meta files_ !!!!**
    * Take care to place your assets in the correct folder, so they will properly versionned

## How it works

### SBT Unity based project folder

A Unity3D project using SBT as build pipeline will have this layout:

- _build.sbt_: standard SBT build file
- **src**: standard SBT source folder
    - **main**: standard SBT main folder
        - **runtime_resources**: folder containing Unity3D assets (Optional)
        - **unity_settings**: folder containings the ProjectSettings file (Optional)
    - **test**: standard SBT test folder
        - **runtime_resources**: folder containing Unity3D assets
        - **unity_settings**: folder containings the ProjectSettings file
- **target**: standard SBT generated folder
    - **workspace**: generated Unity3D folder for main builds
        - **Assets**: standard Unity3D Assets folder
            - **your_project_name**: folder symlinked to **src/main/runtime_resources**
        - **ProjectSettings**: folder symlinked to **src/main/unity_settings**
    - **test-workspace**: generated Unity3D folder for test builds
        - **Assets**: standard Unity3D Assets folder
            - **your_project_name**: folder symlinked to **src/main/runtime_resources**
            - **{your_project_name}_test**: folder symlinked to **src/test/runtime_resources**
        - **ProjectSettings**: folder symlinked to **src/test/unity_settings**

### Workspace generation

As you have noticed, a folder for the Unity3D project is generated and folders are symlinked to your **src** folders. This to makes easy the source control: you can checkout only the assets you want by placing them in the proper src directory.

The other pros with this architecture, is to have separated environment for test and main builds. So your are sure your main assets do not depend on test assets.

For the test-workspace, the plugin will install a package named _com.unity3d.test-tools_ (the version can be configured with `UnityKeys.unityTestToolsVersion` SBT setting.

_This is also a more generic way to see the assets: thoses are middleware independent and can be processed by the middleware you want. Here, we use Unity3D, but you may wish to use CMake to produce C/C++ based assets in a similar way._

## How to contribute

Feel free to fork it and create a pull request !

# FAQ

- #### The workspace generation does not works on Windows, it says that I don't have enough privileges !

On Windows, creating a symlink is an Administrator privileges, so you will have to run your command with an Administrator role. (right click on your shell > Execute as administrator)

- #### SBT did not find the dependency _org.fredericvauchelles.sbt-unity_ !

Did you correctly provided the artifact in your repository ? Or published locally the artifact ?

- #### SBT did not find the dependency _org.fredericvauchelles.sbt-unity-package_ !

Did you correctly provided the artifact in your repository ? Or published locally the artifact ?
This is to be done on the **sbt-unity-package** project.

- #### SBT did not find the dependency _com.unity3d.test-tools_ when generating the test workspace !

The Unity3D Test Tools is provided by Unity3D and I do not provided it. Althought, You must made it available to your dependency resolver by installing it in you preferred repository.

- [Maven file install](http://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html)
- [SBT add local maven repository](http://www.scala-sbt.org/0.13/tutorial/Library-Dependencies.html)

# License

MIT (see license.txt)
