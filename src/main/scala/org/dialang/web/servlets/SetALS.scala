package org.dialang.web.servlets

import java.io.IOException
import java.util.UUID
import javax.servlet.ServletException
import javax.servlet.http.{HttpServletRequest,HttpServletResponse}

class SetALS extends DialangServlet {

  @throws[ServletException]
  @throws[IOException]
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {

    val al = req.getParameter("al")

    val dialangSession = getDialangSession(req)
    dialangSession.adminLanguage = al
    saveDialangSession(dialangSession,req)

    // This updates and gets the updated cookie so we can set it on the response.
    val map = Map("al" -> al)
    val cookie = getUpdatedCookie(req,map,true)

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.addCookie(cookie)
    resp.setContentType("text/html")
    resp.sendRedirect(staticContentRoot + "legend/" + al + ".html")
  }
}
