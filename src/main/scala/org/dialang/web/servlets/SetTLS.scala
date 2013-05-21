package org.dialang.web.servlets

import javax.servlet.ServletException
import javax.servlet.http.{HttpServletRequest,HttpServletResponse,HttpSession}
import java.io.IOException
import java.util.UUID

import org.dialang.common.model.Item

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dialang.web.datacapture.DataLogger

class SetTLS extends DialangServlet {

  private val logger = LoggerFactory.getLogger(getClass)

  @throws[ServletException]
  @throws[IOException]
  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {

    val dialangSession = getDialangSession(req)

    if(dialangSession.adminLanguage == "") {
      if(logger.isInfoEnabled) {
        logger.info("No admin language set at test selection. The session may have timed out. Redirecting to ALS page ...")
      }
      // At this stage, the session should have the al set. This
      // may mean that the user has either jumped to this page or
      // their session had actually timed out.
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.setContentType("text/html")
      resp.sendRedirect("/als.html")
    } else {

      // Zero all state except for the admin language
      dialangSession.startNewTest()

      val tl    = req.getParameter("tl")
      val skill = req.getParameter("skill")

      val sessionId = UUID.randomUUID.toString

      dialangSession.sessionId = sessionId
      dialangSession.scoredItemList = List[Item]()
      dialangSession.testLanguage = tl
      dialangSession.skill = skill.toLowerCase

      saveDialangSession(dialangSession,req)

      dataCapture.createSession(dialangSession,req.getRemoteAddr)

      // This updates and gets the updated cookie so we can set it on the response.
      val cookie = getUpdatedCookie(req,Map("tl" -> tl,
                                            "skill" -> skill.toLowerCase),/*ignoreCurrent=*/false)

      resp.setStatus(HttpServletResponse.SC_OK)
      resp.addCookie(cookie)
      resp.setContentType("text/html")
      resp.sendRedirect("/vsptintro/" + dialangSession.adminLanguage + ".html")
    }
  }
}
