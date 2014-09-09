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