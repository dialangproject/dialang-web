package org.dialang.web.model

case class TES(id: String, al: String, tl: String
                  , skill: String, hideVSPT: Boolean
                  , hideVSPTResult: Boolean, hideSA: Boolean
                  , testDifficulty: String, hideTest: Boolean
                  , hideFeedbackMenu: Boolean, disallowInstantFeedback: Boolean
                  , testCompleteUrl: String) {
  def this() = this("", "", "", "", false, false, false, "medium", false, false, false, "")
}
