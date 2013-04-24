package org.dialang.servlets

import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.{HttpServletRequest,HttpServletResponse}

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import org.dialang.db.DB
import org.dialang.vspt.VSPTUtils

class ScoreVSPT extends DialangServlet {

  val db = DB
  val vsptUtils = new VSPTUtils

  @throws[IOException]
  @throws[ServletException]
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {

    val dialangSession = getDialangSession(req)

    val al = dialangSession.al
    val tl = dialangSession.tl

    val responses = new HashMap[String,Boolean]
    req.getParameterNames.foreach(n => {
      val name = n.asInstanceOf[String]
      if(name.startsWith("word:")) {
        val id = name.split(":")(1)

        val answer = req.getParameter(name) match {
            case "valid" => true
            case "invalid" => false
          }

        responses += (id -> answer)
      }
    })

    // This is a Tuple3 of zscore, meara score and level.
    val (zScore,mearaScore,level) = vsptUtils.getLevel(tl,responses.toMap)

    val sessionId = dialangSession.sessionId

    dataCapture.logVSPTResponsesAndScores(sessionId,responses.toMap,zScore,mearaScore,level)

    val cookie = getUpdatedCookie(req, Map("vsptZScore" -> zScore.toString
                                              ,"vsptMearaScore" -> mearaScore.toString
                                              ,"vsptLevel" -> level
                                              ,"vsptSubmitted" -> "true") )

    resp.addCookie(cookie)

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentType("text/html")
    resp.sendRedirect(staticContentRoot + "vsptfeedback/" + al + "/" + level + ".html")
  }
}
