package org.dialang.web.servlets


import org.dialang.web.datacapture.Saver
import org.dialang.common.model.DialangSession

class Save extends DialangServlet {

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
