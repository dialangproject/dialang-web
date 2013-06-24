package org.dialang.web.servlets

import java.util.UUID

import org.dialang.common.model.ImmutableItem

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

      val tl = params.get("tl") match {
          case Some(s:String) => s
          case None => {
            logger.error("No test language (tl) supplied. Returning 400 (Bad Request) ...")
            halt(400)
          }
        }

      val skill = params.get("skill") match {
          case Some(s:String) => s
          case None => {
            logger.error("No skill supplied. Returning 400 (Bad Request) ...")
            halt(400)
          }
        }

      // Zero all state except for the admin language and sessionId
      dialangSession.startNewTest()

      // The session id persists across tests
      val sessionId = dialangSession.sessionId match {
          case "" => UUID.randomUUID.toString
          case _ => dialangSession.sessionId
        }

      // The pass id correlates with a test run. We always need a
      // new one for each test.
      val passId = UUID.randomUUID.toString

      if(logger.isDebugEnabled) {
        logger.debug("TL: " + tl)
        logger.debug("SKILL: " + skill)
        logger.debug("SESSION ID: " + sessionId)
        logger.debug("PASS ID: " + passId)
      }

      dialangSession.sessionId = sessionId
      dialangSession.passId = passId
      dialangSession.testLanguage = tl
      dialangSession.skill = skill.toLowerCase

      saveDialangSession(dialangSession)

      dataCapture.createSession(dialangSession,request.remoteAddress)

      contentType = "text/plain"
      "success"
    }
  }
}
