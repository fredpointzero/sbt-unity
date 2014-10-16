/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
import org.scalatest.FreeSpec
import sbt._

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

    "should extract the context of settings directories" - {
      "for main context" - {
        assertResult("main") {
          UnityPlugin.extractSettingsDirectoryContext(file("C:\\Users\\Fredpointzero\\AppData\\Local\\Temp\\sbt_a6ca9432\\missing_test\\src\\main\\unity_settings"));
        }
      }

      "for test context" - {
        assertResult("test") {
          UnityPlugin.extractSettingsDirectoryContext(file("C:\\Users\\Fredpointzero\\AppData\\Local\\Temp\\sbt_a6ca9432\\missing_test\\src\\test\\unity_settings"));
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
