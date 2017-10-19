package org.dialang.web.servlets

import scala.collection.JavaConversions._
import org.json4s.{DefaultFormats, Formats}

import org.scalatra.json._

import org.dialang.web.datacapture.Loader
import org.dialang.common.model.DialangSession

class Load extends DialangServlet with JacksonJsonSupport {

  private lazy val loader = new Loader("java:comp/env/jdbc/dialang", "java:comp/env/jdbc/dialangdatacapture")

  protected implicit val jsonFormats: Formats = DefaultFormats

  get("/") {

    val token = params.getOrElse("token", halt(400, "No token supplied"))

    contentType = formats("json")
    loader.loadDialangSession(token, db) match {
        case Some(dialangSession: DialangSession) => {
          saveDialangSession(dialangSession)

          if (dataCapture.deleteToken(token)) {
            dialangSession.toCase
          } else {
            "{\"error\": \"Failed to delete saved session\"}"
          }
        }
        case None => {
          "{\"error\": \"Invalid token supplied\"}"
        }
      }
  }
}
