package org.dialang.web.servlets

import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.{HttpServletRequest,HttpServletResponse}

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import org.dialang.web.db.DB
import org.dialang.web.vspt.VSPTUtils

class ScoreVSPT extends DialangServlet {

  private val db = DB
  private val vsptUtils = new VSPTUtils

  @throws[IOException]
  @throws[ServletException]
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {

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

    val dialangSession = getDialangSession(req)

    // This is a Tuple3 of zscore, meara score and level.
    val (zScore,mearaScore,level) = vsptUtils.getBand(dialangSession.testLanguage,responses.toMap)

    dialangSession.vsptZScore = zScore.toFloat
    dialangSession.vsptMearaScore = mearaScore
    dialangSession.vsptLevel = level
    dialangSession.vsptSubmitted = true

    saveDialangSession(dialangSession,req)

    dataCapture.logVSPTResponses(dialangSession,responses.toMap)
    dataCapture.logVSPTScores(dialangSession)

    val cookie = getUpdatedCookie(req, Map("vsptMearaScore" -> mearaScore.toString,
                                              "vsptLevel" -> level,
                                              "vsptDone" -> "true"))

    resp.addCookie(cookie)

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentType("text/html")
    resp.sendRedirect("/vsptfeedback/" + dialangSession.adminLanguage + "/" + level + ".html")
  }
}
