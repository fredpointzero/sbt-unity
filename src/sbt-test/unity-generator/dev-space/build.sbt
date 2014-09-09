import java.nio.file.{Paths, Files}

name := "devSpace"

version := "0.1"

unitySettings

TaskKey[Unit]("check-symlink") <<= (target, normalizedName) map { (targetDirectory, normName) =>
  val path = Paths.get(targetDirectory.getAbsolutePath + "/unityBuildWorkspace/Assets/" + normName);
  if(!Files.isSymbolicLink(path)) {
    sys.error(s"$path is not a symbolic link");
  }
  ()
}

TaskKey[Unit]("check-unity-project") <<= (target, normalizedName) map { (targetDirectory, normName) =>
  if (!(targetDirectory / "/unityBuildWorkspace/Library/").exists()
    || !(targetDirectory / "/unityBuildWorkspace/ProjectSettings/").exists()) {
    sys.error("Unity project creation failed, (missing Library or ProjectSettings folders)");
  }
  ()
}