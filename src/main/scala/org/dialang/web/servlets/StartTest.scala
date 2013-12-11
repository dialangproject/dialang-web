package org.dialang.web.servlets

import org.dialang.web.db.DBFactory
import org.dialang.web.model.DialangSession
import org.dialang.web.scoring.ScoringMethods

import org.slf4j.LoggerFactory;

class StartTest extends DialangServlet {

  private val logger = LoggerFactory.getLogger(getClass)
  
  private val db = DBFactory.get()
  private val scoringMethods = new ScoringMethods

  get("/") {

    val dialangSession = getDialangSession

    if(dialangSession.testLanguage == "" || dialangSession.skill == "") {
      logger.error("Neither the test language or skill were set in the session. Returning 500 ...")
      halt(500)
    }

    if(dialangSession.scoredItemList.length > 0) {
      logger.error("The scored item list should be empty at this point. Returning 500 ...")
      halt(500)
    }

    val bookletId = scoringMethods.calculateBookletId(dialangSession)

    val bookletLength = db.getBookletLength(bookletId)

    val firstBasketId = db.getBasketIdsForBooklet(bookletId).head

    if(logger.isDebugEnabled) {
      logger.debug("BOOKLET ID: " + bookletId)
      logger.debug("BOOKLET LENGTH: " + bookletLength)
      logger.debug("First Basket Id: " + firstBasketId)
    }

    dialangSession.bookletId = bookletId
    dialangSession.bookletLength = bookletLength
    dialangSession.currentBasketNumber = 0

    saveDialangSession(dialangSession)

    dataCapture.logTestStart(dialangSession.passId)

    contentType = "application/json";
    "{\"totalItems\":\"" + bookletLength.toString + "\",\"startBasket\":\"" + firstBasketId.toString + "\"}"
  }
}
