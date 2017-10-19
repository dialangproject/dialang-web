package org.dialang.web.servlets

import java.util.{Date,UUID}

import org.dialang.common.model.ImmutableItem

class SetTLS extends DialangServlet {

  post("/") {

    logger.debug("settls")

    val dialangSession = getDialangSession

    if (dialangSession.tes.al == "") {
      if (logger.isInfoEnabled) {
        logger.info("No admin language set at test selection. The session may have timed out. Redirecting to ALS page ...")
      }
      // At this stage, the session should have the al set. This
      // may mean that the user has either jumped to this page or
      // their session had actually timed out.
      contentType = "text/html"
      redirect("/dialang-content/als.html")
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
      dialangSession.clearPass()

      // The session id persists across tests (passes)
      val (sessionId, isNewSession) = dialangSession.sessionId match {
          case "" => (UUID.randomUUID.toString, true)
          case _ => (dialangSession.sessionId, false)
        }

      // The pass id correlates with a test run. We always need a
      // new one for each test.
      dialangSession.passId = UUID.randomUUID.toString

      logger.debug("TL: " + tl)
      logger.debug("SKILL: " + skill)
      logger.debug("SESSION ID: " + sessionId)
      logger.debug("PASS ID: " + dialangSession.passId)

      dialangSession.sessionId = sessionId
      dialangSession.tes.tl = tl
      dialangSession.tes.skill = skill.toLowerCase
      dialangSession.ipAddress = request.remoteAddress
      dialangSession.browserLocale = request.locale.toString
      dialangSession.started = new Date

      saveDialangSession(dialangSession)

      if (isNewSession) {
        dataCapture.createSessionAndPass(dialangSession)
      } else {
        dataCapture.createPass(dialangSession)
      }

      contentType = "text/plain"
      "success"
    }
  }
}
