package org.dialang.web.servlets

import org.dialang.db.DBFactory
import org.dialang.common.model.DialangSession

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import org.scalatra.NoContent

class SubmitQuestionnaire extends DialangServlet {

  post("/") {

    val dialangSession = getDialangSession

    dataCapture.storeQuestionnaire(dialangSession.sessionId, params)
    NoContent
  }
}
