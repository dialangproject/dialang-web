package org.dialang.web.servlets

import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.{HttpServletRequest,HttpServletResponse}

import org.dialang.web.db.DB
import org.dialang.web.model.DialangSession
import org.dialang.web.scoring.ScoringMethods

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StartTest extends DialangServlet {

  private val logger = LoggerFactory.getLogger(getClass)
  
  private val db = DB
  private val scoringMethods = new ScoringMethods

  @throws[ServletException]
  @throws[IOException]
  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {

    val dialangSession = getDialangSession(req)

    if(dialangSession.testLanguage == "" || dialangSession.skill == "") {
      // This should not happen. The tl and skill should be set by now.
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

    saveDialangSession(dialangSession,req)

    val cookie = getUpdatedCookie(req,Map("bookletLength" -> bookletLength.toString, "currentBasketNumber" -> "0"))

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.addCookie(cookie)
    resp.setContentType("text/html")
    resp.sendRedirect(staticContentRoot + "baskets/" + dialangSession.adminLanguage + "/" + firstBasketId + ".html")
  }
}
