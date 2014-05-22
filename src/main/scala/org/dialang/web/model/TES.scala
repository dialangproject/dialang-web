package org.dialang.web.model

case class TES(id: String,
                var al: String,
                var tl: String,
                var skill: String,
                hideVSPT: Boolean,
                hideVSPTResult: Boolean,
                hideSA: Boolean,
                hideTest: Boolean,
                testDifficulty: String,
                hideFeedbackMenu: Boolean,
                disallowInstantFeedback: Boolean,
                testCompleteUrl: String) {

  def this() = this("", "", "", "", false, false, false, false, "medium", false, false, "")
  /*
  def toMap = {
    Map(
      "id" -> 
  }
  */
}
