package org.dialang.web.servlets

import org.scalatra.scalate.ScalateSupport

import org.slf4j.LoggerFactory

class SetALS extends DialangServlet with ScalateSupport {

  private val logger = LoggerFactory.getLogger(classOf[SetALS])

  post("/") {

    val al = params("al")

    if (logger.isDebugEnabled) {
      logger.debug("al: " + al);
    }

    // Stash the admin language in the session
    val dialangSession = getDialangSession
    dialangSession.tes.al = al
    saveDialangSession(dialangSession)

    contentType = "text/html"
    mustache("shell", "state" -> "legend",
                      "al" -> al,
                      "disallowInstantFeedback" -> dialangSession.tes.disallowInstantFeedback)
  }
}
