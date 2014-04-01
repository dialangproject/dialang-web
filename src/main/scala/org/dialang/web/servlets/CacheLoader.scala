package org.dialang.web.servlets

import javax.servlet.http.HttpServlet
import org.dialang.web.db.DBFactory

class CacheLoader extends HttpServlet {

  private val db = DBFactory.get()
}
