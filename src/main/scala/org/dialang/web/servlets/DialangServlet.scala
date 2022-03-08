package org.dialang.web.servlets

import org.dialang.common.model.DialangSession
import org.dialang.web.model.InstructorSession
import org.dialang.web.datacapture.DataCapture

import org.scalatra.ScalatraServlet

import org.dialang.db.DBFactory

import grizzled.slf4j.Logging

class DialangServlet extends ScalatraServlet with Logging {

  protected lazy val dataCapture = new DataCapture("java:comp/env/jdbc/dialangdatacapture")

  protected val db = DBFactory.get()

  private val instructorSessionKey = "dialangInstructorSession"

  private val dialangSessionKey = "dialangSession"

  protected def getInstructorSession = session.get(instructorSessionKey)

  protected def saveInstructorSession(instructorSession: InstructorSession): Unit = {

    session += (instructorSessionKey -> instructorSession)
  }

  /**
   * Gets the existing, or creates a new, DialangSession
   */
  protected def getDialangSession = {

    session.get(dialangSessionKey) match {
        case Some(d: DialangSession) => d
        case _ => new DialangSession
      }
  }

  protected def saveDialangSession(dialangSession: DialangSession): Unit = {

    session += (dialangSessionKey -> dialangSession)
  }

  protected def getBaseContentUrl: String = config.getServletContext.getInitParameter("baseContentUrl");
}
