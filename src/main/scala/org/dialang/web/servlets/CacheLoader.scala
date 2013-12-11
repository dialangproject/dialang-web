package org.dialang.web.servlets

import javax.servlet.http.HttpServlet
import org.dialang.web.db.DBFactory
import org.dialang.web.datacapture.DataLogger

class CacheLoader extends HttpServlet {
  private val db = DBFactory.get()
  private val dl = DataLogger
}
