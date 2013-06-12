package org.dialang.web.model

import org.dialang.common.model.Item
import org.dialang.web.util.DialangLogger

class DialangSession extends Serializable with DialangLogger {

  var userId:String = ""

  var consumerKey:String = ""

  var adminLanguage:String = ""

  var sessionId:String = ""

  var passId:String = ""

  var testLanguage:String = ""

  var skill:String = ""

  var vsptSubmitted:Boolean = false

  var saSubmitted:Boolean = false

  var vsptZScore:Float = 0F

  var vsptMearaScore:Int = 0

  var vsptLevel:String = "V0"

  var saPPE:Float = 0F

  var saLevel:String = ""

  var itemGrade:Int = 0

  var itemLevel:String = ""

  var saResponses:Map[String,Int] = Map()

  var bookletId:Int = 0

  var bookletLength:Int = 0

  var currentBasketNumber:Int = 0

  var scoredItemList:List[Item] = List[Item]()

  var scoredBasketList:List[Basket] = List[Basket]()

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
    saResponses = Map()
    bookletId = 0
    bookletLength = 0
    currentBasketNumber = 0
    scoredItemList = List[Item]()
    scoredBasketList = List[Basket]()
  }
}
