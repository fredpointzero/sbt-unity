/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
import org.scalatest._
import sbt.file

class UnityWrapperTest extends FreeSpec {
  "UnityWrapper" - {
    "should detect Unity's executable default path" - {
      "on windows" - {
        assertResult(file("C:\\Program Files (x86)\\Unity\\Editor\\Unity.exe")) {
          UnityWrapper detectUnityExecutableFromOS "windows";
        }
      }

      "on osx" - {
        assertResult(file("/Applications/Unity/Unity.app/Contents/MacOS/Unity")) {
          UnityWrapper detectUnityExecutableFromOS "mac";
        }
      }
    }

    "should fail to detect Unity's executable default path on other OS" - {
      intercept[RuntimeException] {
        UnityWrapper detectUnityExecutableFromOS "plop";
      }
    }
  }
}
