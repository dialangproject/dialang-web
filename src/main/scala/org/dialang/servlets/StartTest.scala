package org.dialang.servlets

import javax.servlet.http._

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import org.dialang.model.DialangSession
import org.dialang.scoring.ScoringMethods

class StartTest extends DialangServlet {

  val scoringMethods = new ScoringMethods

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
    val al = req.getParameter("al")

    val cookieMap = getCookieMap(req)
    val tl = cookieMap.getOrElse("tl","")
    val skill = cookieMap.getOrElse("skill","")
    val vsptSubmitted = cookieMap.getOrElse("vsptSubmitted","")
    val saSubmitted = cookieMap.getOrElse("saSubmitted","")
    val vsptZScore = cookieMap.getOrElse("vsptZScore","")
    val saPPE = cookieMap.getOrElse("saPPE","")
    val bookletId = cookieMap.getOrElse("bookletId","")

    if(tl == "" || skill == "" || vsptSubmitted == "" || saSubmitted == "" || vsptZScore == "" || saPPE == "" || bookletId == "") {
      // This should not happen. The skill should be set by now.
    }

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentType("text/html")
    resp.getWriter.write("<html><body><h1>Starting test in " + tl + "#" + skill + ". Booklet ID: " + bookletId + "</h1></body></html>")
    resp.getWriter.close()
    //resp.sendRedirect("content/testintro/" + al + ".html")
  }
}
