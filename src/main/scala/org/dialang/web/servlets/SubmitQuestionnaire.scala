package org.dialang.web.servlets

import org.dialang.db.DBFactory
import org.dialang.common.model.DialangSession

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import org.scalatra.NoContent

import org.slf4j.LoggerFactory

class SubmitQuestionnaire extends DialangServlet {

  private val log = LoggerFactory.getLogger(classOf[SubmitQuestionnaire])

  post("/") {

    val dialangSession = getDialangSession

    dataCapture.storeQuestionnaire(dialangSession.sessionId, params)
    NoContent
  }
}
