package org.dialang.web.servlets

import org.dialang.db.DBFactory
import org.dialang.common.model.DialangSession

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

import org.scalatra.NoContent

class SubmitQuestionnaire extends DialangServlet {

  post("/") {

    dataCapture.storeQuestionnaire(getDialangSession.sessionId, params.toMap)
    NoContent
  }
}
