package com.mindwaves_studio.sbt_unity.test

import com.mindwaves_studio.sbt_unity.UnityPlugin
import org.scalatest.FreeSpec
import sbt._

/**
 * Created by Fredpointzero on 10/09/2014.
 */
class UnityPluginTest extends FreeSpec {
  "UnityPlugin" - {
    "should extract the context of source directories" - {
      "for main context" - {
        assertResult("main") {
          UnityPlugin.extractSourceDirectoryContext(file("C:\\Users\\Fredpointzero\\AppData\\Local\\Temp\\sbt_a6ca9432\\missing_test\\src\\main\\runtime_resources"));
        }
      }

      "for test context" - {
        assertResult("test") {
          UnityPlugin.extractSourceDirectoryContext(file("C:\\Users\\Fredpointzero\\AppData\\Local\\Temp\\sbt_a6ca9432\\missing_test\\src\\test\\runtime_resources"));
        }
      }
    }
    "should return null on other directories" - {
      assertResult(null) {
        UnityPlugin.extractSourceDirectoryContext(file("C:\\Users\\Fredpointzero\\AppData\\Local\\Temp\\sbt_a6ca9432\\missing_test\\src\\test\\java"));
      }
    }
  }
}
