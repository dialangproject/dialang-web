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

    val cookieMap = getCookieMap(req)
    val tl = cookieMap.getOrElse("tl","")
    val skill = cookieMap.getOrElse("skill","")
    val vsptSubmitted = cookieMap.getOrElse("vsptSubmitted","")
    val vsptZScore = cookieMap.getOrElse("vsptZScore","")

    if(tl == "" || skill == "" || vsptSubmitted == "" || vsptZScore == "") {
      // This should not happen. The skill should be set by now.
    }

    val dialangSession = new DialangSession(cookieMap)

    val saPPE = scoringMethods.getSaPPE(skill,responses.toMap)

    dialangSession.saSubmitted = true
    dialangSession.saPPE = saPPE

    val bookletId = scoringMethods.calculateBookletId(new DialangSession(cookieMap))

    println("BOOKLET ID: " + bookletId)

    val cookie = getUpdatedCookie(req,Map("saPPE" -> saPPE.toString,"saSubmitted" -> "true","bookletId" -> bookletId.toString))

    resp.addCookie(cookie)

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentType("text/html")
    resp.sendRedirect("content/testintro/" + al + ".html")
  }
}
