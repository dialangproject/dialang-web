package org.dialang.web.servlets

import scala.collection.JavaConversions._

import org.dialang.web.datacapture.Saver
import org.dialang.web.model.DialangSession

import org.slf4j.LoggerFactory;

class Save extends DialangServlet {

  private val logger = LoggerFactory.getLogger(getClass)
  private lazy val saver = new Saver("java:comp/env/jdbc/dialangdatacapture")

  get("/") {

    val dialangSession = getDialangSession

    if (dialangSession.vsptSubmitted || dialangSession.saSubmitted || dialangSession.scoredItemList.length > 0) {
      val token = saver.save(dialangSession) match {
          case Some(token: String) => {
            token
          }
          case None => {
            ""
          }
        }
      contentType = "application/json"
      "{\"token\": \"" + token + "\"}"
    } else {
      // Nothing to save yet
      contentType = "application/json"
      "{\"token\": \"nothing to save\"}"
    }
  }
}
