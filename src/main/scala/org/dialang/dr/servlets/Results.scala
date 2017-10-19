package org.dialang.dr.servlets

import org.scalatra.ScalatraServlet
import org.scalatra._

import org.dialang.dr.db.DB
import org.dialang.dr.model.Session

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.NoSuchAlgorithmException
import org.apache.commons.codec.binary.Base64

import grizzled.slf4j.Logging

class Results extends ScalatraServlet with Logging {

  private val db = DB

  post("/") {

    val consumerKey = params.getOrElse("consumer_key", halt(400))
    val userId = params.getOrElse("user_id", halt(400))

    // Get the base64 encoded hash of the consumer key supplied by the client
    val theirHash = params.getOrElse("hash", halt(400))

    val secret = db.getSecret(consumerKey) match {
        case Some(s: String) => s
        case None => {
          logger.error("Failed to lookup secret for consumer key '" + consumerKey + "'") 
          halt(500)
        }
      }

    try {

      // Now hash the consumer key using the secret

      val secretKeySpec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA1")

      val mac = Mac.getInstance("HmacSHA1")

      mac.init(secretKeySpec)

      val ourHash = Base64.encodeBase64String(mac.doFinal(consumerKey.getBytes("UTF-8")))

      // If the hashes are different at this point, authn has failed.
      if(theirHash != ourHash) {
        halt(400)
      }
    } catch {
      case nsae: NoSuchAlgorithmException => {
        logger.error("Failed to lookup HMAC-SHA1 implementation", nsae)
        halt(500)
      }
      case e: Exception => {
        logger.error("Caught exception whilst comparing hashes", e)
        halt(500)
      }
    }

    // Authenticated. Grab the user's response data and return as XML.
    val sessions = db.getSessionsForUserId(userId, consumerKey) match {
        case Some(l: List[Session]) => l
        case None => halt(400)
      }
    
    var xml = "<xml>\n\t<user id=\"" + userId + "\">\n"
    sessions.foreach(session => {
      xml += "\t\t<session id=\"" + session.id + "\" ip-address=\"" + session.ipAddress + "\" started-millis-since-epoch=\"" + session.startedMillisSinceEpoch + "\">\n"
      session.passes.foreach(pass => {
        xml += "\t\t\t<pass id=\"" + pass.id + "\" al=\"" + pass.al + "\" tl=\"" + pass.tl + "\" skill=\"" + pass.skill + "\" />\n"
        if (pass.vsptResponseData.isDefined) {
          val vspt = pass.vsptResponseData.get
          xml += "\t\t\t\t<vspt>\n"
          xml += "\t\t\t\t\t<z-score>" + vspt.zScore + "</z-score>\n"
          xml += "\t\t\t\t\t<meara-score>" + vspt.mearaScore + "</meara-score>\n"
          xml += "\t\t\t\t\t<level>" + vspt.level + "</level>\n"
          xml += "\t\t\t\t</vspt>\n"
        }

        if (pass.saResponseData.isDefined) {
          val sa = pass.saResponseData.get
          xml += "\t\t\t\t<sa>\n"
          xml += "\t\t\t\t\t<ppe>" + sa.ppe + "</ppe>\n"
          xml += "\t\t\t\t</sa>\n"
        }

        if (pass.itemResponseData.isDefined) {
          val items = pass.itemResponseData.get
          xml += "\t\t\t\t<items start-millis-since-epoch=\"" + items.startMillisSinceEpoch + "\" finish-millis-since-epoch=\"" + items.finishMillisSinceEpoch + "\">\n"
          xml += "\t\t\t\t\t<grade>" + items.grade + "</grade>\n"
          xml += "\t\t\t\t\t<level>" + items.level + "</level>\n"
          xml += "\t\t\t\t</items>\n"
        }
      })
      xml += "\t\t</session>\n"
    })
    xml += "\t</user>\n</xml>\n"

    contentType = "text/xml"
    Ok(xml)
  }
}
