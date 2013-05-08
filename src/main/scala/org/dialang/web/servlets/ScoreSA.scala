package org.dialang.web.servlets

import javax.servlet.http._
import javax.servlet.ServletException

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import org.dialang.web.model.DialangSession
import org.dialang.web.scoring.ScoringMethods

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ScoreSA extends DialangServlet {

  private val logger = LoggerFactory.getLogger(getClass)

  private val scoringMethods = new ScoringMethods

  @throws[ServletException]
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {

    val responses = {
        val tmp = new HashMap[String,Boolean]
        req.getParameterNames.foreach( n => {
          val name = n.asInstanceOf[String]
          if(name.startsWith("statement:")) {
            val wid = name.split(":")(1)

            val answer = req.getParameter(name) match {
                case "yes" => true
                case "no" => false
              }

            tmp += (wid -> answer)
          }
        })
        tmp.toMap
      }

    val dialangSession = getDialangSession(req)

    if(dialangSession.testLanguage == "" || dialangSession.skill == "") {
      // This should not happen. The skill should be set by now.
      throw new ServletException("testLanguage and skill are not set");
    }

    val (saPPE,saLevel) = scoringMethods.getSaPPEAndLevel(dialangSession.skill,responses)

    dialangSession.saPPE = saPPE
    dialangSession.saSubmitted = true
    dialangSession.saLevel = saLevel

    saveDialangSession(dialangSession,req)

    dataCapture.logSAResponses(dialangSession,responses.toMap)
    dataCapture.logSAPPE(dialangSession)

    val cookie = getUpdatedCookie(req,Map("saLevel" -> saLevel))
    resp.addCookie(cookie)

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentType("text/html")
    resp.sendRedirect(staticContentRoot + "testintro/" + dialangSession.adminLanguage + ".html")
  }
}
