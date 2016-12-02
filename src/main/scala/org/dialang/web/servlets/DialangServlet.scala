package org.dialang.web.servlets

import org.dialang.common.model.DialangSession
import org.dialang.web.model.InstructorSession
import org.dialang.web.datacapture.DataCapture

import org.scalatra.ScalatraServlet

import org.slf4j.LoggerFactory

import org.dialang.db.DBFactory

class DialangServlet extends ScalatraServlet {

  private val logger = LoggerFactory.getLogger(getClass)

  protected lazy val dataCapture = new DataCapture("java:comp/env/jdbc/dialangdatacapture")

  protected val db = DBFactory.get()

  private val instructorSessionKey = "dialangInstructorSession"

  private val dialangSessionKey = "dialangSession"

  protected def getInstructorSession = session.get(instructorSessionKey)

  protected def saveInstructorSession(instructorSession: InstructorSession) {

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

  protected def saveDialangSession(dialangSession: DialangSession) {

    session += (dialangSessionKey -> dialangSession)
  }
}
