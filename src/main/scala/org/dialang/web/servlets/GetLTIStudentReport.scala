package org.dialang.web.servlets

import org.dialang.web.model.InstructorSession

import org.scalatra.scalate.ScalateSupport

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

        if (logger.isDebugEnabled) {
          logger.debug("al: " + session.al)
          logger.debug("fromDate: " + fromDate)
          logger.debug("toDate: " + toDate);
        }

        //val list: List[Tuple5[String, String, String, String, String]] = dataCapture.getScores(session.consumerKey, fromDate, toDate)
        //val list: List[Tuple5[String, String, String, String, String]] = dataCapture.getScores(session.consumerKey, fromDate, toDate)
        val list = dataCapture.getScores(session.consumerKey, fromDate, toDate)

        val csv = new StringBuilder
        csv.append("pass_id,user_id,vspt_level,sa_level,test_level\n")

        list.forEach(t => {

          csv.append(t._1).append(",")
            .append(t._2) .append(",")
            .append(t._3) .append(",")
            .append(t._4) .append(",")
            .append(t._5) .append("\n")
        })
      }
      case None => {
        logger.error("No instructor session. Halting ...")
        halt(403)
      }
    }
  }
}
