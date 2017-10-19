package org.dialang.web.servlets

import org.dialang.db.DBFactory
import org.dialang.common.model.DialangSession
import org.dialang.scoring.ScoringMethods

class StartTest extends DialangServlet {

  private val scoringMethods = new ScoringMethods

  get("/") {

    val dialangSession = getDialangSession

    if (dialangSession.tes.tl == "" || dialangSession.tes.skill == "") {
      logger.error("Neither the test language or skill were set in the session. Returning 500 ...")
      halt(500)
    }

    if (dialangSession.scoredItemList.length > 0) {
      logger.error("The scored item list should be empty at this point. Returning 500 ...")
      halt(500)
    }

    val bookletId = scoringMethods.calculateBookletId(dialangSession)

    val bookletLength = db.getBookletLength(bookletId)

    val firstBasketId = db.getBasketIdsForBooklet(bookletId).head

    logger.debug("BOOKLET ID: " + bookletId)
    logger.debug("BOOKLET LENGTH: " + bookletLength)
    logger.debug("First Basket Id: " + firstBasketId)

    dialangSession.bookletId = bookletId
    dialangSession.bookletLength = bookletLength

    saveDialangSession(dialangSession)

    dataCapture.logTestStart(dialangSession.passId, dialangSession.bookletId, dialangSession.bookletLength)

    contentType = "application/json";
    "{\"totalItems\":\"" + bookletLength.toString + "\",\"startBasket\":\"" + firstBasketId.toString + "\"}"
  }
}
