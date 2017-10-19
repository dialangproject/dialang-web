package org.dialang.web.servlets

import scala.collection.JavaConversions._
import org.json4s.{DefaultFormats, Formats}

import org.scalatra.json._

import org.dialang.web.model.InstructorSession

class GetLTIUserNames extends DialangServlet with JacksonJsonSupport {

  protected implicit val jsonFormats: Formats = DefaultFormats

  get("/") {

    getInstructorSession match {

      case Some(session: InstructorSession) => {

        val consumerKey = session.consumerKey

        val tuples = dataCapture.getLTIUserNames(consumerKey)

        val userNames = tuples.map(t => t._1 + " " + t._2 + " (User ID:" + t._3 + ")")

        contentType = formats("json")
        userNames
      }
      case _ => {
        logger.error("No instructor session. This is incorrect. Returning 403 ...")
        halt(403)
      }
    }
  }
}
