/*
 * Copyright (c) 2014 Frédéric Vauchelles
 *
 * See the file license.txt for copying permission.
 */
import org.scalatest.FreeSpec

class UnityPluginTest extends FreeSpec {
  "UnityPlugin" - {
    "should extract the context of source directories" - {
      "for main context" - {
        assertResult("main") {
          UnityPlugin.extractAnyDirectoryContext(
            sbt.file("C:\\Users\\Fredpointzero\\AppData\\Local\\Temp\\sbt_a6ca9432\\missing_test\\src\\main\\runtime_resources"),
            UnityPlugin.SOURCES_FOLDER_NAME
          );
        }
      }

      "for test context" - {
        assertResult("test") {
          UnityPlugin.extractAnyDirectoryContext(
            sbt.file("C:\\Users\\Fredpointzero\\AppData\\Local\\Temp\\sbt_a6ca9432\\missing_test\\src\\test\\runtime_resources"),
            UnityPlugin.SOURCES_FOLDER_NAME
          );
        }
      }
    }

    "should return null on other directories" - {
      assertResult(null) {
        UnityPlugin.extractAnyDirectoryContext(
          sbt.file("C:\\Users\\Fredpointzero\\AppData\\Local\\Temp\\sbt_a6ca9432\\missing_test\\src\\test\\java"),
          UnityPlugin.SOURCES_FOLDER_NAME
        );
      }
    }

    "should extract the context of settings directories" - {
      "for main context" - {
        assertResult("main") {
          UnityPlugin.extractAnyDirectoryContext(
            sbt.file("C:\\Users\\Fredpointzero\\AppData\\Local\\Temp\\sbt_a6ca9432\\missing_test\\src\\main\\unity_settings"),
            UnityPlugin.SETTINGS_FOLDER_NAME
          );
        }
      }

      "for test context" - {
        assertResult("test") {
          UnityPlugin.extractAnyDirectoryContext(
            sbt.file("C:\\Users\\Fredpointzero\\AppData\\Local\\Temp\\sbt_a6ca9432\\missing_test\\src\\test\\unity_settings"),
            UnityPlugin.SETTINGS_FOLDER_NAME
          );
        }
      }
    }

    "should return null on other directories" - {
      assertResult(null) {
        UnityPlugin.extractAnyDirectoryContext(
          sbt.file("C:\\Users\\Fredpointzero\\AppData\\Local\\Temp\\sbt_a6ca9432\\missing_test\\src\\test\\java"),
          UnityPlugin.SETTINGS_FOLDER_NAME
        );
      }
    }
  }
}
