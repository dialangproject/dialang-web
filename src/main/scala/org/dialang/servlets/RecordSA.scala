package org.dialang.servlets

import javax.servlet.http._

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import org.dialang.model.DialangSession
import org.dialang.scoring.ScoringMethods

class RecordSA extends DialangServlet {

  val scoringMethods = new ScoringMethods

  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {

    val dialangSession = getDialangSession(req)

    val al = dialangSession.al

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

    if(dialangSession.tl == "" || dialangSession.skill == "") {
      // This should not happen. The skill should be set by now.
    }

    val (saPPE,saLevel) = scoringMethods.getSaPPEAndLevel(dialangSession.skill,responses.toMap)

    dataCapture.logSAResponsesAndPPE(dialangSession.sessionId,responses.toMap,saPPE)

    val cookie = getUpdatedCookie(req,Map("saPPE" -> saPPE.toString,"saSubmitted" -> "true","saLevel" -> saLevel))

    resp.addCookie(cookie)

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentType("text/html")
    resp.sendRedirect(staticContentRoot + "testintro/" + al + ".html")
  }
}
