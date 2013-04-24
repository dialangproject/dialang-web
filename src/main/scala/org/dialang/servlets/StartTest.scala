package org.dialang.servlets

import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.{HttpServletRequest,HttpServletResponse}

import org.dialang.db.DB
import org.dialang.model.DialangSession
import org.dialang.scoring.ScoringMethods

class StartTest extends DialangServlet {
  
  val db = DB
  val scoringMethods = new ScoringMethods

  @throws[ServletException]
  @throws[IOException]
  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {

    val dialangSession = getDialangSession(req)

    if(dialangSession.tl == "" || dialangSession.skill == "") {
      // This should not happen. The tl and skill should be set by now.
    }

    val bookletId = scoringMethods.calculateBookletId(dialangSession)

    val bookletLength = db.getBookletLength(bookletId)

    println("BOOKLET ID: " + bookletId)
    println("BOOKLET LENGTH: " + bookletLength)

    val basketId = db.getBasketIdsForBooklet(bookletId).head
    println("First Basket Id: " + basketId)

    val cookie = getUpdatedCookie(req,Map("bookletId" -> bookletId.toString,"bookletLength" -> bookletLength.toString, "currentBasketNumber" -> "0"))

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.addCookie(cookie)
    resp.setContentType("text/html")
    resp.sendRedirect(staticContentRoot + "baskets/" + dialangSession.al + "/" + basketId + ".html")
  }
}
