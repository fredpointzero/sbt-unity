/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
import java.nio.file.Files

import sbt.Keys._
import sbt._
import sbt.inc.Analysis

object UnityPlugin extends sbt.Plugin{
  import UnityPlugin.UnityKeys._

  object UnityKeys {
    // Paths
    val unitySource = SettingKey[Seq[File]]("unity-source", "Default Unity source directories")
    val workspaceDirectory = SettingKey[File]("workspace-directory", "Directory of the Unity workspace")

    // Tasks
    val generateWorkspace = TaskKey[File]("generate-workspace", "Generate a Unity workspace")

    // Unity Options
    val crossPlatform = SettingKey[UnityWrapper.TargetPlatform.Value]("cross-platform", "Target platform for the build")
    val unityEditorExecutable = SettingKey[File]("unity-editor-executable", "Path to the Unity editor executable to use")
    val unityTestToolsVersion = SettingKey[String]("unity-test-tools-version", "Version of the Unity test tools package to use")
    val unityPackageToolsVersion = SettingKey[String]("unity-package-tools-version", "Version of the sbt-unity-package")

    val unityUnitTestFilters = SettingKey[Seq[String]]("unity-unit-test-filters", "Filter fo Unity Test Tools unit tests")
    val unityUnitTestCategories = SettingKey[Seq[String]]("unity-unit-test-categories", "Categories fo Unity Test Tools unit tests")
    val unityIntegrationTestScenes = SettingKey[Seq[String]]("unity-integration-test-scenes", "Scenes to execute during the integration tests")
    val unityIntegrationTestPlatform = SettingKey[String]("unity-integration-test-platform", "Platform to use for the integration tests")
  }

  def unityPlayerSettings: Seq[Setting[_]] = unityCommonSettings ++ Seq(
    // Cross building
    crossPlatform := UnityWrapper.TargetPlatform.None,
    artifactName := {
      (scalaVersion:ScalaVersion, module:ModuleID, artifact:Artifact) => {
        import artifact._
        val classifierStr = classifier match { case None => ""; case Some(c) => "-" + c }
        artifact.name + "_" + crossPlatform.value + "-" + module.revision + classifierStr + "." + artifact.extension
      }
    }
  ) ++ unityPlayerSettingsIn(Compile) ++ unityPlayerSettingsIn(Test)

  private def unityPlayerSettingsIn(c:Configuration) =
    inConfig(c)(Seq(
      crossTarget := target.value / crossPlatform.value.toString(),

      compile := compileTask.value,
      products <<= productsTask,
      artifact := artifactSetting.value,
      run := runTask.value
    ))

  def unityPackageSettings: Seq[Setting[_]] = unityCommonSettings ++ Seq(
    crossVersion := CrossVersion.Disabled,
    artifactName := {
      (scalaVersion:ScalaVersion, module:ModuleID, artifact:Artifact) => {
        import artifact._
        val classifierStr = classifier match { case None => ""; case Some(c) => "-" + c }
        artifact.name + "-" + module.revision + classifierStr + "." + artifact.extension
      }
    },
    mappings in (Compile, packageBin) := Seq((file(""), s"Assets/${normalizedName.value}")),
    skip in run := true,
    // There is no cross build constraints for unity package
    crossTarget := target.value
  ) ++ unityPackageSettingsIn(Compile) ++ unityPackageSettingsIn(Test)

  private def unityPackageSettingsIn(c:Configuration) =
    inConfig(c)(Seq(
      // Tasks
      products <<= Def.task { Nil },
      compile := {
        val x1 = generateWorkspace.value;
        Analysis.Empty;
      },
      artifact in packageBin := { (artifact in packageBin).value.copy(`type` = "unitypackage", extension = "unitypackage"); },
      packageBin := {
        val x1 = compile.value;
        UnityWrapper.buildUnityPackage(
          workspaceDirectory.value,
          (artifactPath in packageBin).value,
          file((artifactPath in packageBin).value.toString() + ".log"),
          (mappings in packageBin).value map { a => a._2 },
          streams.value.log);

        (artifactPath in packageBin).value;
      }
    ))

  private def unityCommonSettings: Seq[Setting[_]] = Seq(
    // Unity options
    unityEditorExecutable := UnityWrapper.detectUnityExecutable,
    mappings.in(Compile, packageBin) <<= (mappings.in(Compile, packageBin), streams) map { (f, s) =>
      s.log.warn(s"Mapping with ${f.size} file")
      for((file, path) <- f) s.log.warn(s"$file -> $path");
      f
    },

    unityUnitTestFilters := Seq(),
    unityUnitTestCategories := Seq(),
    unityIntegrationTestScenes := Seq(),
    unityIntegrationTestPlatform := "Windows",
    unityPackageToolsVersion := version.value,
    unityTestToolsVersion := "1.4.1",

    // Add build pipeline package
    libraryDependencies ++= {
      val v = unityPackageToolsVersion.value;
      val org = "org.fredericvauchelles";
      val a = "sbt-unity-package"
      if (organization.value != org && name.value != a && version.value != v)
        Seq(org % a % v artifacts Artifact (a, "unitypackage", "unitypackage"))
      else
        Seq()
    },
    libraryDependencies += "com.unity3d" % "test-tools" % unityTestToolsVersion.value % Test artifacts Artifact("test-tools", "unitypackage", "unitypackage")

  ) ++ inConfig(Compile)(Seq(
    unitySource := Seq(sourceDirectory.value / SOURCES_FOLDER_NAME, sourceDirectory.value / SETTINGS_FOLDER_NAME),
    unmanagedSourceDirectories := unitySource.value,

    // Workspace
    workspaceDirectory := target.value / "workspace",
    generateWorkspace := generateWorkspaceTaskIn(Compile).value
  )) ++ inConfig(Test)(Seq(
    unitySource := Seq(
      sourceDirectory.value / SOURCES_FOLDER_NAME,
      (sourceDirectory in Compile).value / SOURCES_FOLDER_NAME,
      sourceDirectory.value / SETTINGS_FOLDER_NAME
    ),
    unmanagedSourceDirectories := unitySource.value,

    // Force complete unit test in test task
    unityUnitTestFilters in test := Seq(),
    unityUnitTestCategories in test := Seq(),

    sbt.Keys.test := testTaskIn(test).value,
    sbt.Keys.testOnly := testTaskIn(testOnly).value,

    // Workspace
    workspaceDirectory := target.value / "test-workspace",
    generateWorkspace := generateWorkspaceTaskIn(Test).value
  ))

  def extractSourceDirectoryContext(path:File):String =
    extractAnyDirectoryContext(path, SOURCES_FOLDER_NAME);

  def extractSettingsDirectoryContext(path:File):String =
    extractAnyDirectoryContext(path, SETTINGS_FOLDER_NAME);

  private val SOURCES_FOLDER_NAME = "runtime_resources";
  private val SETTINGS_FOLDER_NAME = "unity_settings";
  private val ANY_PATH_PATTERN = "([^\\\\/]*)(?:\\\\|/)([^\\\\/]*)$".r;

  private def extractAnyDirectoryContext(path:File, folderName:String):String = {
    val matches = ANY_PATH_PATTERN findAllIn(path toString);
    if (matches.hasNext && matches.group(2) == folderName) {
      val context = matches.group(1);
      return context;
    }
    else {
      return null;
    }
  }

  private def testTaskIn(key:Scoped) = Def.task {
    val x1 = generateWorkspace.value;

    // Unit Tests
    {
      val filters = if((unityUnitTestFilters in key).value.size > 0) Seq("-filter=" + (unityUnitTestFilters in key).value.mkString(",")) else Seq()
      val categories = if((unityUnitTestCategories in key).value.size > 0) Seq("-categories=" + (unityUnitTestCategories in key).value.mkString(",")) else Seq()
      UnityWrapper.callUnityEditorMethod(
        workspaceDirectory.value,
        workspaceDirectory.value / "test.log",
        streams.value.log,
        "UnityTest.Batch.RunUnitTests",
        Seq("-resultFilePath=" + workspaceDirectory.value / "../unit-test-report.xml") ++ filters ++ categories);
    }

    // Integration Tests
    {
      val resultDirectory = workspaceDirectory.value / "../resultDirectory";
      if (!resultDirectory.exists()) {
        resultDirectory.mkdirs();
      }
      val scenes = if((unityIntegrationTestScenes in key).value.size > 0) Seq("-testscenes=" + (unityIntegrationTestScenes in key).value.mkString(",")) else Seq()
      val platform = if((unityIntegrationTestPlatform in key).value.size > 0) Seq("-targetPlatform=" + (unityIntegrationTestPlatform in key).value) else Seq()
      UnityWrapper.callUnityEditorMethod(
        workspaceDirectory.value,
        workspaceDirectory.value / "test.log",
        streams.value.log,
        "UnityTest.Batch.RunIntegrationTests",
        Seq("-resultsFileDirectory=" + resultDirectory) ++ scenes ++ platform,
        false);
    }
  }

  private def artifactSetting = Def.setting { Artifact.apply(name.value, UnityWrapper.extensionForPlatform(crossPlatform.value), "jar", s"${configuration}-$crossPlatform"); }

  private def productsTask = Def.task {
    val x1 = compile.value;
    Seq(crossTarget.value)
  }

  private def runTask = Def.task {
    val x1 = compile.value;
    val executable = crossTarget.value / (normalizedName.value + "." + UnityWrapper.extensionForPlatform(crossPlatform.value));
    executable.toString() !;
  }

  private def compileTask = Def.task {
    if(!crossTarget.value.exists()) {
      crossTarget.value.mkdirs();
    }
    val x1 = generateWorkspace.value;
    UnityWrapper.buildUnityPlayer(workspaceDirectory.value, file(crossTarget.value.toString() + ".log"), crossPlatform.value, crossTarget.value / normalizedName.value, streams.value.log);
    Analysis.Empty;
  }

  private def generateWorkspaceTaskIn(c:Configuration) = Def.task {
    val assetDirectory = workspaceDirectory.value / "Assets";
    // Make directories if necessary
    if (!assetDirectory.exists()) {
      assetDirectory.mkdirs();
    }

    // Create the Unity project
    if (!(workspaceDirectory.value / "Library").exists()) {
      UnityWrapper.createUnityProjectAt(workspaceDirectory.value, target.value / s"${workspaceDirectory.value}.log", streams.value.log);
    }

    val libFiles:Seq[File] = update.value.matching(
        artifactFilter(`type`= "unitypackage", extension = "unitypackage") &&
        configurationFilter(name = c.name)
        ) ++
      Option(unmanagedBase.value.listFiles).toList.flatten
    if (libFiles != null)  {
      for (packageFile:File <- libFiles.filter(_.ext == "unitypackage")) {
        streams.value.log.info(s"importing lib: $packageFile")
        UnityWrapper.importPackage(workspaceDirectory.value, workspaceDirectory.value / s"import-${packageFile.name}.log", packageFile, streams.value.log);
      }
    }

    for (sourceDir <- unitySource.value) {
      val sourcesContext = extractSourceDirectoryContext(sourceDir);
      if (sourcesContext != null) {
        val suffix = if (sourcesContext == "main") "" else s"_${sourcesContext}";
        val linkedDirectory = assetDirectory / s"${normalizedName.value}$suffix";
        // Replace the target and create the symlink
        if (linkedDirectory.exists() && !Files.isSymbolicLink(linkedDirectory toPath)) {
          streams.value.log.info(s"Replacing directory $linkedDirectory by a symlink");
          linkedDirectory.delete();
        }
        if (!linkedDirectory.exists()) {
          if(sourceDir.exists()) {
            Files.createSymbolicLink(linkedDirectory toPath, sourceDir toPath);
          }
          else {
            streams.value.log.info(s"Skipping $linkedDirectory because $sourceDir does not exists");
          }
        }
        else {
          streams.value.log.info(s"Skipping $linkedDirectory as it already exists");
        }
      }

      val settingsContext = extractSettingsDirectoryContext(sourceDir);
      if (settingsContext != null && sourceDir.exists()) {
        val targetLink = workspaceDirectory.value / "ProjectSettings";
        if(targetLink.exists()) {
          streams.value.log.info(s"Deleting existing setting: $targetLink");
          IO.delete(targetLink);
        }
        Files.createSymbolicLink(targetLink toPath, sourceDir toPath);
      }
    }

    workspaceDirectory.value;
  }
}
