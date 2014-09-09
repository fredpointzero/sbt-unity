package com.mindwaves_studio.sbt_unity.test

import com.mindwaves_studio.sbt_unity._
import org.scalatest._
import sbt.file

/**
 * Created by Fredpointzero on 09/09/2014.
 */
class UnityWrapperTest extends FreeSpec {
  "UnityWrapper" - {
    "should detect Unity's executable default path" - {
      "on windows" - {
        assertResult(file("C:\\Program Files (x86)\\Unity\\Editor\\Unity.exe")) {
          UnityWrapper detectUnityExecutableFromOS "windows";
        }
      }

      "on osx" - {
        assertResult(file("/Application/Unity/Editor/Unity")) {
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
