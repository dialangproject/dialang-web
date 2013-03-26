package org.dialang.servlets

import javax.servlet.ServletException
import javax.servlet.http.{HttpServletRequest,HttpServletResponse}
import java.io.IOException
import java.util.UUID

class SetTLS extends DialangServlet {

  @throws[ServletException]
  @throws[IOException]
  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {

    val dialangSession = getDialangSession(req)

    val al    = req.getParameter("al")
    val tl    = req.getParameter("tl")
    val skill = req.getParameter("skill")

    val sessionId = dataCapture.createSession(dialangSession.userId,al,tl,skill,req.getRemoteAddr)

    // This updates and gets the updated cookie so we can set it on the response.
    val cookie = getUpdatedCookie(req,Map("sessionId" -> sessionId.toString,
                                            "tl" -> tl,
                                            "skill" -> skill.toLowerCase),/*ignoreCurrent=*/true)

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.addCookie(cookie)
    resp.setContentType("text/html")
    resp.sendRedirect(staticContentRoot + "vsptintro/" + al + ".html")
  }
}
