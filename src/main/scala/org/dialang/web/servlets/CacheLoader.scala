package org.dialang.web.servlets

import javax.servlet.http.HttpServlet
import org.dialang.web.db.DB
import org.dialang.web.datacapture.DataLogger

class CacheLoader extends HttpServlet {
  private val db = DB
  private val dl = DataLogger
}
