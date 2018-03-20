package org.dialang.web.servlets

import org.scalatra.{BadRequest, Forbidden}
import org.scalatra.scalate.ScalateSupport

import java.util.{Date, TimeZone}
import java.text.DateFormat

import scala.collection.JavaConversions._

import org.apache.commons.codec.digest.HmacUtils
import org.apache.commons.codec.digest.HmacAlgorithms._

class GetStudentReport extends DialangServlet {//with ScalateSupport {

  get("/") {

    val consumerKey = params.get("consumerKey")
    val hash = params.get("hash")
    val userId = params.getOrElse("userId", "")

    if (consumerKey.isEmpty || hash.isEmpty) {
      BadRequest("You must provide a consumerKey and hash")
    } else {
      logger.debug("consumerKey: " + consumerKey)
      logger.debug("hash: " + hash)
      logger.debug("userId: " + userId)

      val secret = db.getSecret(consumerKey.get)

      if (secret.isEmpty) {
        BadRequest("consumerKey not recognised")
      } else {
        val testHash = {
            val hmacUtils = new HmacUtils(HMAC_SHA_1, secret.get)
            if (userId .length > 0) hmacUtils.hmacHex(consumerKey.get + userId)
            else hmacUtils.hmacHex(consumerKey.get)
          }

        if (testHash != hash.get) {
          logger.debug("Hashes don't match.")
          logger.debug("Theirs: " + hash)
          logger.debug("Ours: " + testHash)
          Forbidden("Hashes don't match")
        } else {
          val csv = new StringBuilder
          csv.append("user_id,first_name,last_name,al,tl,vspt_level,sa_level,test_level,started\n")

          val formatter = DateFormat.getInstance
          formatter.setTimeZone(TimeZone.getTimeZone("UTC"))

          dataCapture.getScores(consumerKey=consumerKey.get, userId=userId).foreach(t => {

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
      }
    }
  }
}
