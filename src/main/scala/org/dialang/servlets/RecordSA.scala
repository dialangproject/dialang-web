package org.dialang.servlets

import javax.servlet.http._

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import org.dialang.model.DialangSession
import org.dialang.scoring.ScoringMethods

class RecordSA extends DialangServlet {

  val scoringMethods = new ScoringMethods

  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val al = req.getParameter("al")
    val cheatlevel = req.getParameter("cheatlevel")

    var responses = new HashMap[String,Boolean]
    req.getParameterNames.foreach( n => {
      val name = n.asInstanceOf[String]
      if(name.startsWith("statement:")) {
        val wid = name.split(":")(1)

        val answer = req.getParameter(name) match {
              case "yes" => true
              case "no" => false
          }

        responses += (wid -> answer)
      }
    })

    val dialangSession = getDialangSession(req)

    if(dialangSession.tl == "" || dialangSession.skill == "") {
      // This should not happen. The skill should be set by now.
    }

    val saPPE = scoringMethods.getSaPPE(dialangSession.skill,responses.toMap)

    dataCapture.logSAResponsesAndPPE(dialangSession.sessionId,responses.toMap,saPPE)

    dialangSession.saSubmitted = true
    dialangSession.saPPE = saPPE

    val bookletId = scoringMethods.calculateBookletId(dialangSession)

    val bookletLength = 0//db.getBookletLength(bookletId)

    println("BOOKLET ID: " + bookletId)

    val cookie = getUpdatedCookie(req,Map("saPPE" -> saPPE.toString,"saSubmitted" -> "true","bookletId" -> bookletId.toString,"bookletLength" -> bookletLength.toString, "currentBasketNumber" -> "0"))

    resp.addCookie(cookie)

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentType("text/html")
    resp.sendRedirect(staticContentRoot + "testintro/" + al + ".html")
  }
}
