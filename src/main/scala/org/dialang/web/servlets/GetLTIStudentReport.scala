package org.dialang.web.servlets

import org.dialang.web.model.InstructorSession

import org.scalatra.scalate.ScalateSupport

import java.util.{Date, Locale, TimeZone}
import java.text.DateFormat

import scala.collection.JavaConversions._

import org.slf4j.LoggerFactory

import net.oauth.OAuth

class GetLTIStudentReport extends DialangServlet with ScalateSupport {

  private val logger = LoggerFactory.getLogger(classOf[GetLTIStudentReport])

  get("/") {

    getInstructorSession match {

      case Some(session: InstructorSession) => {
        val fromDate = params("fromDate")
        val toDate = params("toDate")
        val userId = params("userId")

        if (logger.isDebugEnabled) {
          logger.debug("al: " + session.al)
          logger.debug("fromDate: " + fromDate)
          logger.debug("toDate: " + toDate);
          logger.debug("userId: " + userId);
        }

        val list = dataCapture.getScores(session.consumerKey, fromDate, toDate, userId)

        val csv = new StringBuilder
        csv.append("user_id,vspt_level,sa_level,test_level,started\n")

        list.foreach(t => {

          val formatter = DateFormat.getInstance
          formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
          val startedDate = formatter.format(new Date(t._5.asInstanceOf[Long]*1000L)) + " UTC"

          csv.append(t._1).append(",")
            .append(t._2) .append(",")
            .append(t._3) .append(",")
            .append(t._4) .append(",")
            .append(startedDate) .append("\n")
        })
        contentType = "text/csv"
        csv
      }
      case None => {

        logger.error("No instructor session. Halting ...")
        halt(403)
      }
    }
  }
}
