package org.dialang.web.servlets

class SkipVSPT extends DialangServlet {

  get("/") { getDialangSession.vsptSkipped = true }
}
