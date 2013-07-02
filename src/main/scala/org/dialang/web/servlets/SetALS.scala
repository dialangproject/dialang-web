package org.dialang.web.servlets

import org.scalatra.scalate.ScalateSupport

class SetALS extends DialangServlet with ScalateSupport {

  post("/") {

    val al = params("al")

    // Stash the admin language in the session
    val dialangSession = getDialangSession
    dialangSession.adminLanguage = al
    saveDialangSession(dialangSession)

    contentType = "text/html"
    mustache("shell","state" -> "legend", "al" -> al, "instantFeedbackDisabled" -> dialangSession.instantFeedbackDisabled)
  }
}
