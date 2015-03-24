package org.dialang.web.servlets

class SkipSA extends DialangServlet {

  get("/") { getDialangSession.saSkipped = true }
}
