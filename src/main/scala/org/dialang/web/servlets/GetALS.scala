package org.dialang.web.servlets

import org.scalatra.scalate.ScalateSupport

class GetALS extends DialangServlet with ScalateSupport {

  get("/") {

    contentType = "text/html"
    mustache("shell", "dataUrl" -> "${getBaseContentUrl()}/getals", "state" -> "als")
  }
}
