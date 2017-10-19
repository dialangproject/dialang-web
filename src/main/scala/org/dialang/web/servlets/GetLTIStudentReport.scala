package org.dialang.web.servlets

import org.dialang.web.model.InstructorSession

import org.scalatra.scalate.ScalateSupport

import java.util.{Date, Locale, TimeZone}
import java.text.DateFormat

import scala.collection.JavaConversions._

import net.oauth.OAuth

class GetLTIStudentReport extends DialangServlet with ScalateSupport {

  get("/") {

    getInstructorSession match {

      case Some(session: InstructorSession) => {

        val fromDate = params("fromDate")
        val toDate = params("toDate")
        val userId = params("userId")

        logger.debug("al: " + session.al)
        logger.debug("fromDate: " + fromDate)
        logger.debug("toDate: " + toDate)
        logger.debug("userId: " + userId)

        val csv = new StringBuilder
        csv.append("user_id,first_name,last_name,al,tl,vspt_level,sa_level,test_level,started\n")

        val formatter = DateFormat.getInstance
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"))

        dataCapture.getScores(session.consumerKey, fromDate, toDate, userId, session.resourceLinkId).foreach(t => {

          val startedDate = formatter.format(new Date(t._9.asInstanceOf[Long]*1000L)) + " UTC"

          csv.append(t._1).append(",")
            .append(t._2) .append(",")
            .append(t._3) .append(",")
            .append(t._4) .append(",")
            .append(t._5) .append(",")
            .append(t._6) .append(",")
            .append(t._7) .append(",")
            .append(t._8) .append(",")
            .append(startedDate) .append("\n")
        })
        contentType = "text/csv"
        csv
      }
      case _ => {

        logger.error("No instructor session. Halting ...")
        halt(403)
      }
    }
  }
}
