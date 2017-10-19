package org.dialang.web.servlets

import org.scalatra.scalate.ScalateSupport

class SetALS extends DialangServlet with ScalateSupport {

  post("/") {

    val al = params("al")

    logger.debug("al: " + al)

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
