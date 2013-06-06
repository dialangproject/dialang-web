package org.dialang.web.servlets

import java.util.UUID

import org.dialang.common.model.Item

import org.slf4j.LoggerFactory;

import org.dialang.web.datacapture.DataLogger

class SetTLS extends DialangServlet {

  private val logger = LoggerFactory.getLogger(getClass)

  post("/") {

    val dialangSession = getDialangSession

    if(dialangSession.adminLanguage == "") {
      if(logger.isInfoEnabled) {
        logger.info("No admin language set at test selection. The session may have timed out. Redirecting to ALS page ...")
      }
      // At this stage, the session should have the al set. This
      // may mean that the user has either jumped to this page or
      // their session had actually timed out.
      contentType = "text/html"
      redirect("/als.html")
    } else {

      // Zero all state except for the admin language
      dialangSession.startNewTest()

      val tl    = params("tl")
      val skill = params("skill")

      val sessionId = UUID.randomUUID.toString

      if(logger.isDebugEnabled) {
        logger.debug("TL: " + tl)
        logger.debug("SKILL: " + skill)
        logger.debug("SESSION ID: " + sessionId)
      }

      dialangSession.sessionId = sessionId
      dialangSession.scoredItemList = List[Item]()
      dialangSession.testLanguage = tl
      dialangSession.skill = skill.toLowerCase

      saveDialangSession(dialangSession)

      dataCapture.createSession(dialangSession,request.remoteAddress)

      // This updates and gets the updated cookie so we can set it on the response.
      //val cookie = getUpdatedCookie(req,Map("tl" -> tl,
      //                                      "skill" -> skill.toLowerCase),/*ignoreCurrent=*/false)

      //resp.addCookie(cookie)
      contentType = "text/plain"
      "success"
    }
  }
}
