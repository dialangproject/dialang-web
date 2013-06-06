package org.dialang.web.servlets

import org.scalatra.scalate.ScalateSupport

class SetALS extends DialangServlet with ScalateSupport {

  post("/") {

    val al = params("al")

    val dialangSession = getDialangSession
    dialangSession.adminLanguage = al
    saveDialangSession(dialangSession)

    // This updates and gets the updated cookie so we can set it on the response.
    val map = Map("state" -> "legend","al" -> al)
    //val cookie = getUpdatedCookie(map,true)

    //response.addCookie(cookie)

    contentType = "text/html"

    mustache("shell",map.toSeq:_*)
  }
}
