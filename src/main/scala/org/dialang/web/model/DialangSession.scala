package org.dialang.web.model

import org.dialang.common.model.ImmutableItem
import org.dialang.web.util.DialangLogger

class DialangSession extends Serializable with DialangLogger {

  var userId = ""
  var consumerKey = ""
  var adminLanguage = ""
  var sessionId = ""
  var ipAddress = ""
  var started = 0L
  var passId = ""
  var testLanguage = ""
  var skill = ""
  var instantFeedbackDisabled = false
  var vsptSubmitted = false
  var saSubmitted = false
  var vsptZScore = 0F
  var vsptMearaScore = 0
  var vsptLevel = "V0"
  var saPPE = 0F
  var saLevel = ""
  var itemGrade = 0
  var itemLevel = ""
  var saResponses = Map[String, Int]()
  var bookletId = 0
  var bookletLength = 0
  var currentBasketNumber = 0
  var scoredItemList = List[ImmutableItem]()
  var scoredBasketList = List[Basket]()

  /**
   * Resets all state except adminLanguage
   */
  def startNewTest() {

    debug("Resetting all state except the adminLanguage and sessionId ...")

    passId = ""
    testLanguage = ""
    skill = ""
    vsptSubmitted = false
    saSubmitted = false
    vsptZScore = 0F
    vsptMearaScore = 0
    vsptLevel = "V0"
    saPPE = 0F
    saLevel = ""
    itemGrade = 0
    itemLevel = ""
    saResponses = Map[String, Int]()
    bookletId = 0
    bookletLength = 0
    currentBasketNumber = 0
    scoredItemList = List[ImmutableItem]()
    scoredBasketList = List[Basket]()
  }
}
