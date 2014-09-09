package com.mindwaves_studio.sbt_unity.test

import org.scalatest._

/**
 * Created by Fredpointzero on 09/09/2014.
 */
class UnityWrapperTest extends FreeSpec {
  "A Set" - {
    "when empty" - {
      "should have size 0" in {
        assert(Set.empty.size == 0)
      }

      "should produce NoSuchElementException when head is invoked" in {
        intercept[NoSuchElementException] {
          Set.empty.head
        }
      }
    }
  }
}
