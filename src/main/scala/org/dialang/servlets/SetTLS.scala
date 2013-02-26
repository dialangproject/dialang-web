package org.dialang.servlets

import javax.servlet.http._
import java.util.UUID

class SetTLS extends DialangServlet {

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
      val al    = req.getParameter("al")
      val tl    = req.getParameter("tl")
      val skill = req.getParameter("skill")

      val sessionId = UUID.randomUUID.toString

      val cookie = getUpdatedCookie(req,Map("sessionId" -> sessionId,"tl" -> tl,"skill" -> skill.toLowerCase),/*ignoreCurrent=*/true)

      resp.setStatus(HttpServletResponse.SC_OK)
      resp.addCookie(cookie)
      resp.setContentType("text/html")
      resp.sendRedirect("content/vsptintro/" + al + ".html")
  }
}
