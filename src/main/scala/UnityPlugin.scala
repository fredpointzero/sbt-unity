/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */

import java.nio.file.Files

import UnityWrapper.TargetPlatform.TargetPlatform
import sbt.Keys._
import sbt._
import sbt.inc.Analysis

object UnityPlugin extends sbt.Plugin {

  import UnityPlugin.UnityKeys._

  object Hook extends Enumeration {
    type Hook = Value;
    val PreCompile, PostCompile, PreTest, PostTest, PrePackage = Value;
  }

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
    val unityUnitTestSkip = SettingKey[Boolean]("unity-unit-test-skip", "Skip unit test")
    val unityIntegrationTestScenes = SettingKey[Seq[String]]("unity-integration-test-scenes", "Scenes to execute during the integration tests")
    val unityIntegrationTestPlatform = SettingKey[String]("unity-integration-test-platform", "Platform to use for the integration tests")
    val unityIntegrationTestSkip = SettingKey[Boolean]("unity-integration-test-skip", "Skip integration test")

    val unityHooks = SettingKey[Seq[(Hook.Value, String, Seq[String], Boolean)]]("unity-hooks", "Hooks for Unity methods")
  }

  def unityPlayerSettings: Seq[Setting[_]] = PlayerPipelineAPI.settings;

  def unityPackageSettings: Seq[Setting[_]] = PackagePipelineAPI.settings;

  object PlayerPipelineAPI {

    /* -------- SETTINGS -------- */

    def settings: Seq[Setting[_]] = CommonPipelineAPI.settings ++ Seq(
      // Cross building
      crossPlatform := UnityWrapper.TargetPlatform.None,
      crossTarget := target.value / crossPlatform.value.toString(),
      artifactName := artifactNameSetting.value
    ) ++ unityPlayerSettingsIn(Compile) ++ unityPlayerSettingsIn(Test)

    private def unityPlayerSettingsIn(c: Configuration) =
      inConfig(c)(Seq(
        sbt.Keys.compile := runPostCompileHooksIn(c).value,
        products <<= productsTask,
        artifact := artifactSetting.value,
        run := runTask.value,
        packageBin := packageBinTaskIn(c).value
      ))

    /* -------- TASKS -------- */

    private def artifactSetting = Def.setting {
      Artifact.apply(name.value, UnityWrapper.extensionForPlatform(crossPlatform.value), "jar", s"${configuration}-$crossPlatform");
    }

    private def artifactNameSetting = Def.setting {
      (scalaVersion: ScalaVersion, module: ModuleID, artifact: Artifact) => {
        import artifact._
        val classifierStr = classifier match {
          case None => "";
          case Some(c) => "-" + c
        }
        artifact.name + "_" + crossPlatform.value + "-" + module.revision + classifierStr + "." + artifact.extension
      }
    }

    private def runTask = Def.task {
      sbt.Keys.compile.value;
      val executable = getExecutableForRun(crossTarget.value, normalizedName.value, crossPlatform.value);
      executable.toString() !;
    }

    private def runPreCompileHooksIn(c:Configuration) = Def.task {
      (streams in c).value.log.info(s"Run Pre Compile Hook In $c");
      (UnityKeys.generateWorkspace in c).value;
      if (!(crossTarget in c).value.exists()) {
        (crossTarget in c).value.mkdirs();
      }
      CommonPipelineAPI.runHooks(
        Hook.PreCompile,
        (unityHooks in c).value,
        (workspaceDirectory in c).value,
        (streams in c).value.log
      );
    }

    private def compileTaskIn(c: Configuration) = Def.task {
      (streams in c).value.log.info(s"Compile in $c");
      runPreCompileHooksIn(c).value;
      compile(
        (crossTarget in c).value,
        (workspaceDirectory in c).value,
        (crossPlatform in c).value,
        (normalizedName in c).value,
        (streams in c).value.log
      );
    }

    private def runPostCompileHooksIn(c:Configuration) = Def.task {
      (streams in c).value.log.info(s"Run Post Compile Hook In $c");
      val result = compileTaskIn(c).value;
      CommonPipelineAPI.runHooks(
        Hook.PostCompile,
        (unityHooks in c).value,
        (workspaceDirectory in c).value,
        (streams in c).value.log
      );
      result;
    }

    private def runPrePackageHooksIn(c:Configuration) = Def.task {
      (streams in c).value.log.info(s"Run Pre Package Hook In $c");
      (sbt.Keys.compile in c).value;
      CommonPipelineAPI.runHooks(
        Hook.PrePackage,
        (unityHooks in c).value,
        (workspaceDirectory in c).value,
        (streams in c).value.log
      );
    }

    private def packageBinTaskIn(c: Configuration) = Def.task {
      (streams in c).value.log.info(s"Package In $c");
      runPrePackageHooksIn(c).value;
      sbt.Keys.packageBin.value;
    }

    private def productsTask = Def.task {
      sbt.Keys.compile.value;
      Seq(crossTarget.value)
    }

    /* -------- API -------- */

    private def getExecutableForRun(
                                     crossTarget: File,
                                     normalizedName: String,
                                     crossPlatform: TargetPlatform
                                     ): File = {
      crossTarget / (normalizedName + "." + UnityWrapper.extensionForPlatform(crossPlatform));
    }

    private def compile(
                         crossTarget: File,
                         workspaceDirectory: File,
                         crossPlatform: TargetPlatform,
                         normalizedName: String,
                         log: Logger
                         ): Analysis = {

      if (!crossTarget.exists()) {
        crossTarget.mkdirs();
      }
      UnityWrapper.buildUnityPlayer(
        workspaceDirectory,
        file(crossTarget.toString() + ".log"),
        crossPlatform,
        crossTarget / normalizedName,
        log
      );

      Analysis.Empty;
    }
  }

  object PackagePipelineAPI {

    /* -------- SETTINGS -------- */

    def settings: Seq[Setting[_]] = CommonPipelineAPI.settings ++ Seq(
      crossVersion := CrossVersion.Disabled,
      mappings in(Compile, packageBin) := Seq((file(""), s"Assets/${normalizedName.value}")),
      skip in run := true,
      // There is no cross build constraints for unity package
      crossTarget := target.value
    ) ++ unityPackageSettingsIn(Compile) ++ unityPackageSettingsIn(Test)

    private def unityPackageSettingsIn(c: Configuration) =
      inConfig(c)(Seq(
        // Tasks
        products <<= Def.task {
          Nil
        },
        artifactName := artifactNameSettingIn(c).value,
        compile := runPostCompileHookTaskIn(c).value,
        artifact in packageBin := artifactSettingIn(c).value,
        packageBin := packageBinTaskIn(c).value
      ));

    /* -------- TASKS -------- */

    private def artifactNameSettingIn(c: Configuration) = Def.setting {
      (scalaVersion: ScalaVersion, module: ModuleID, artifact: Artifact) => {
        import artifact._
        val classifierStr = classifier match {
          case None => "";
          case Some(c) => "-" + c
        }
        artifact.name + "-" + module.revision + classifierStr + "." + artifact.extension
      }
    }

    private def artifactSettingIn(c: Configuration) = Def.setting {
      (artifact in(c, packageBin)).value.copy(`type` = "unitypackage", extension = "unitypackage");
    }

    private def runPreCompileHookTaskIn(c:Configuration) = Def.task {
      (streams in c).value.log.info(s"Run Pre Compile Hook In $c");
      (UnityKeys.generateWorkspace in c).value;
      CommonPipelineAPI.runHooks(
        Hook.PreCompile,
        (unityHooks in c).value,
        (workspaceDirectory in c).value,
        (streams in c).value.log
      );
    }

    private def compileTaskIn(c: Configuration) = Def.task {
      (streams in c).value.log.info(s"Compile in $c");
      runPreCompileHookTaskIn(c).value;
      Analysis.Empty;
    }

    private def runPostCompileHookTaskIn(c:Configuration) = Def.task {
      (streams in c).value.log.info(s"Run Post Compile Hook In $c");
      val result = compileTaskIn(c).value;
      CommonPipelineAPI.runHooks(
        Hook.PostCompile,
        (unityHooks in c).value,
        (workspaceDirectory in c).value,
        (streams in c).value.log
      );
      result;
    }

    private def runPrePackageHooksIn(c:Configuration) = Def.task {
      (streams in c).value.log.info(s"Run Pre Package Hook In $c");
      (sbt.Keys.compile in c).value;
      CommonPipelineAPI.runHooks(
        Hook.PrePackage,
        (unityHooks in c).value,
        (workspaceDirectory in c).value,
        (streams in c).value.log
      );
    }

    private def packageBinTaskIn(c: Configuration) = Def.task {
      (streams in c).value.log.info(s"Package in $c");
      runPrePackageHooksIn(c).value;

      UnityWrapper.buildUnityPackage(
        (workspaceDirectory in c).value,
        (artifactPath in(c, packageBin)).value,
        file((artifactPath in(c, packageBin)).value.toString() + ".log"),
        (mappings in(c, packageBin)).value map { a => a._2},
        (streams in c).value.log);

      (artifactPath in(c, packageBin)).value;
    }

    /* -------- API -------- */
  }

  object CommonPipelineAPI {

    val SOURCES_FOLDER_NAME = "runtime_resources";
    val SETTINGS_FOLDER_NAME = "unity_settings";
    val PLUGINS_FOLDER_NAME = "plugins_resources";
    val PLUGINS_EDITOR_FOLDER_NAME = "plugins_editor_resources";
    val ANY_PATH_PATTERN = "([^\\\\/]*)(?:\\\\|/)([^\\\\/]*)$".r;

    val SEARCHED_FOLDERS = Map(
      SOURCES_FOLDER_NAME -> { (normName: String, contextSuffix: String) => s"Assets/${normName}${contextSuffix}"},
      PLUGINS_FOLDER_NAME -> { (normName: String, contextSuffix: String) => s"Assets/Plugins/${normName}${contextSuffix}"},
      PLUGINS_EDITOR_FOLDER_NAME -> { (normName: String, contextSuffix: String) => s"Assets/Plugins/Editor/${normName}${contextSuffix}"},
      SETTINGS_FOLDER_NAME -> { (normName: String, contextSuffix: String) => "ProjectSettings"}
    );

    /* -------- SETTINGS -------- */

    def settings: Seq[Setting[_]] = Seq(
      // Unity options
      unityEditorExecutable := UnityWrapper.detectUnityExecutable,
      mappings.in(Compile, packageBin) <<= (mappings.in(Compile, packageBin), streams) map { (f, s) =>
        s.log.warn(s"Mapping with ${f.size} file")
        for ((file, path) <- f) s.log.warn(s"$file -> $path");
        f
      },

      unityUnitTestFilters := Seq(),
      unityUnitTestCategories := Seq(),
      unityUnitTestSkip := false,
      unityIntegrationTestScenes := Seq(),
      unityIntegrationTestPlatform := "Windows",
      unityIntegrationTestSkip := false,
      unityPackageToolsVersion := version.value,
      unityTestToolsVersion := "1.4.1",
      unityHooks := Seq(),

      // Add build pipeline package
      libraryDependencies ++= {
        val v = unityPackageToolsVersion.value;
        val org = "org.fredericvauchelles";
        val a = "sbt-unity-package"
        if (organization.value != org && name.value != a && version.value != v)
          Seq(org % a % v artifacts Artifact(a, "unitypackage", "unitypackage"))
        else
          Seq()
      },
      libraryDependencies += "com.unity3d" % "test-tools" % unityTestToolsVersion.value % Test artifacts Artifact("test-tools", "unitypackage", "unitypackage")

    ) ++ inConfig(Compile)(Seq(
      unitySource := Seq(
        sourceDirectory.value / SETTINGS_FOLDER_NAME,
        sourceDirectory.value / SOURCES_FOLDER_NAME,
        sourceDirectory.value / PLUGINS_FOLDER_NAME,
        sourceDirectory.value / PLUGINS_EDITOR_FOLDER_NAME
      ),
      unmanagedSourceDirectories := unitySource.value,

      // Workspace
      workspaceDirectory := target.value / "workspace",
      UnityKeys.generateWorkspace := generateWorkspaceTaskIn(Compile).value
    )) ++ inConfig(Test)(Seq(
      unitySource := Seq(
        sourceDirectory.value / SETTINGS_FOLDER_NAME,
        sourceDirectory.value / SOURCES_FOLDER_NAME,
        (sourceDirectory in Compile).value / SOURCES_FOLDER_NAME,
        sourceDirectory.value / PLUGINS_FOLDER_NAME,
        (sourceDirectory in Compile).value / PLUGINS_FOLDER_NAME,
        sourceDirectory.value / PLUGINS_EDITOR_FOLDER_NAME,
        (sourceDirectory in Compile).value / PLUGINS_EDITOR_FOLDER_NAME
      ),
      unmanagedSourceDirectories := unitySource.value,

      // Force complete unit test in test task
      unityUnitTestFilters in test := Seq(),
      unityUnitTestCategories in test := Seq(),

      sbt.Keys.test := runAllTestIn(test).value,
      sbt.Keys.testOnly := runAllTestIn(testOnly).value,

      // Workspace
      workspaceDirectory := target.value / "test-workspace",
      UnityKeys.generateWorkspace := generateWorkspaceTaskIn(Test).value
    ))

    /* -------- TASKS -------- */

    private def runPreTestHookTaskIn(s: Scoped) = Def.task {
      (streams in s).value.log.info(s"Run Pre Test Hook In $s");
      (sbt.Keys.compile in s).value;
      runHooks(Hook.PreTest, (unityHooks in s).value, (workspaceDirectory in s).value, (streams in s).value.log);
    }

    private def runUnitTestTaskIn(s: Scoped) = Def.task {
      (streams in s).value.log.info(s"Run Unit Test In $s");
      runPreTestHookTaskIn(s).value;
      if (!(unityUnitTestSkip in s).value) {
        runUnitTest(
          (unityUnitTestFilters in s).value,
          (unityUnitTestCategories in s).value,
          (workspaceDirectory in s).value,
          (streams in s).value.log
        );
      }
    }

    private def runIntegrationTestTaskIn(s: Scoped) = Def.task {
      (streams in s).value.log.info(s"Run Integration Test In $s");
      runUnitTestTaskIn(s).value;
      if (!(unityIntegrationTestSkip in s).value) {
        runIntegrationTest(
          (unityIntegrationTestScenes in s).value,
          (unityIntegrationTestPlatform in s).value,
          (workspaceDirectory in s).value,
          (streams in s).value.log
        );
      }
    }

    private def runPostTestTaskHookIn(s: Scoped) = Def.task {
      (streams in s).value.log.info(s"Run Post Test Hook In $s");
      runIntegrationTestTaskIn(s).value;
      runHooks(Hook.PostTest, (unityHooks in s).value, (workspaceDirectory in s).value, (streams in s).value.log);
    }

    private def runAllTestIn(s: Scoped) = Def.task {
      (streams in s).value.log.info(s"Run All Test In $s");
      runPostTestTaskHookIn(s).value;
    }

    private def generateWorkspaceTaskIn(c: Configuration) = Def.task {
      (streams in c).value.log.info(s"Generate Workspace Task In $c");
      generateWorkspace(
        (workspaceDirectory in c).value,
        (target in c).value,
        (unmanagedBase in c).value,
        (update in c).value,
        c.name,
        (unitySource in c).value,
        (normalizedName in c).value,
        (streams in c).value.log
      );

      (workspaceDirectory in c).value;
    }

    /* -------- API -------- */

    /** Run the Unity Editor methods hooks
      *
      * @param hookType type of the hook to execute
      * @param hooks hook definitions
      * @param workspaceDirectory directory of the workspace to execute hooks on
      * @param log logger
      */
    def runHooks(
                  hookType: Hook.Hook,
                  hooks: Seq[(Hook.Value, String, Seq[String], Boolean)],
                  workspaceDirectory: File,
                  log: Logger) {
      for ((hook: Hook.Hook, method: String, args: Seq[String], quit: Boolean) <- hooks.filter(_._1 == hookType)) {
        log.info(s"[$hook] Calling $method(${args.reduce((l, r) => s"$l,$r")}})");
        UnityWrapper.callUnityEditorMethod(
          workspaceDirectory,
          workspaceDirectory / s"${hook}-${method}.log",
          log,
          method,
          args,
          quit
        );
      }
    }

    def extractAnyDirectoryContext(path: File, folderName: String): String = {
      val matches = ANY_PATH_PATTERN findAllIn (path toString);
      if (matches.hasNext && matches.group(2) == folderName) {
        val context = matches.group(1);
        return context;
      }
      else {
        return null;
      }
    }

    private def runUnitTest(
                             unityUnitTestFilters: Seq[String],
                             unityUnitTestCategories: Seq[String],
                             workspaceDirectory: File,
                             log: Logger
                             ): Unit = {
      val filters = if (unityUnitTestFilters.size > 0) Seq("-filter=" + unityUnitTestFilters.mkString(",")) else Seq()
      val categories = if (unityUnitTestCategories.size > 0) Seq("-categories=" + unityUnitTestCategories.mkString(",")) else Seq()
      UnityWrapper.callUnityEditorMethod(
        workspaceDirectory,
        workspaceDirectory / "test.log",
        log,
        "UnityTest.Batch.RunUnitTests",
        Seq("-resultFilePath=" + workspaceDirectory / "../unit-test-report.xml") ++ filters ++ categories);
    }

    private def runIntegrationTest(
                                    unityIntegrationTestScenes: Seq[String],
                                    unityIntegrationTestPlatform: String,
                                    workspaceDirectory: File,
                                    log: Logger
                                    ): Unit = {
      val resultDirectory = workspaceDirectory / "../resultDirectory";
      if (!resultDirectory.exists()) {
        resultDirectory.mkdirs();
      }
      val scenes = if (unityIntegrationTestScenes.size > 0) Seq("-testscenes=" + unityIntegrationTestScenes.mkString(",")) else Seq()
      val platform = if (unityIntegrationTestPlatform.size > 0) Seq("-targetPlatform=" + unityIntegrationTestPlatform) else Seq()
      UnityWrapper.callUnityEditorMethod(
        workspaceDirectory,
        workspaceDirectory / "test.log",
        log,
        "UnityTest.Batch.RunIntegrationTests",
        Seq("-resultsFileDirectory=" + resultDirectory) ++ scenes ++ platform,
        false);
    }

    /** Generate the workspace for Unity
      *
      * - Symlink runtime_resources
      * - Symlink unity_settings
      * - Create Unity project
      *
      * @param workspaceDirectory directory to use as workspace
      * @param target target directory of sbt project
      * @param unmanagedBase directory of unmanaged libraries
      * @param update update report of the task
      * @param configurationName name of the current configuration
      * @param unitySource source directories for the Unity project
      * @param normalizedName normalized name of the project
      * @param log logger
      */
    private def generateWorkspace(
                                   workspaceDirectory: File,
                                   target: File,
                                   unmanagedBase: File,
                                   update: UpdateReport,
                                   configurationName: String,
                                   unitySource: Seq[File],
                                   normalizedName: String,
                                   log: Logger): Unit = {

      // Create the Unity project
      if (!(workspaceDirectory / "Library").exists()) {
        UnityWrapper.createUnityProjectAt(workspaceDirectory, target / s"${workspaceDirectory}.log", log);
      }

      val libFiles: Seq[File] = update.matching(
        artifactFilter(`type` = "unitypackage", extension = "unitypackage") &&
          configurationFilter(name = configurationName)
      ) ++
        Option(unmanagedBase.listFiles).toList.flatten
      if (libFiles != null) {
        for (packageFile: File <- libFiles.filter(_.ext == "unitypackage")) {
          log.info(s"importing lib: $packageFile")
          UnityWrapper.importPackage(workspaceDirectory, workspaceDirectory / s"import-${packageFile.name}.log", packageFile, log);
        }
      }

      val linkToPerform = unitySource
        .filter(sourceDir => SEARCHED_FOLDERS.exists(folder => extractAnyDirectoryContext(sourceDir, folder._1) != null))
        .collect({
        case sourceDir => {
          val contextPair = SEARCHED_FOLDERS.collectFirst({
            case folder if extractAnyDirectoryContext(sourceDir, folder._1) != null =>
              (extractAnyDirectoryContext(sourceDir, folder._1), folder._2)
          }).head;
          val contextSuffix = if (contextPair._1 == "main") "" else s"_${contextPair._1}";
          (sourceDir, workspaceDirectory / contextPair._2(normalizedName, contextSuffix));
        }
      });

      for ((sourceDir, linkDir) <- linkToPerform) {
        // Replace the target and create the symlink
        if (linkDir.exists() && !Files.isSymbolicLink(linkDir toPath)) {
          log.info(s"Replacing directory $linkDir by a symlink");
          linkDir.delete();
        }
        if (!linkDir.exists()) {
          if (sourceDir.exists()) {
            val parentLinkDir = file(linkDir.getParent());
            if (!parentLinkDir.exists()) {
              parentLinkDir.mkdirs();
            }
            Files.createSymbolicLink(linkDir toPath, sourceDir toPath);
          }
          else {
            log.info(s"Skipping $linkDir because $sourceDir does not exists");
          }
        }
        else {
          log.info(s"Skipping $linkDir as it already exists");
        }
      }
    }
  }

}
