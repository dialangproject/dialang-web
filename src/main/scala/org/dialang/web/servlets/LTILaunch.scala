package org.dialang.web.servlets

import net.oauth._
import net.oauth.server.OAuthServlet
import net.oauth.signature.OAuthSignatureMethod

import org.dialang.web.db.DBFactory

import scala.collection.JavaConversions._

import org.slf4j.LoggerFactory

import org.scalatra.scalate.ScalateSupport

import java.util.UUID

class LTILaunch extends DialangServlet with ScalateSupport {

  private val logger = LoggerFactory.getLogger(classOf[LTILaunch])

  val db = DBFactory.get()

	post("/") {

    if (logger.isDebugEnabled) logger.debug("lti post")

    val message = OAuthServlet.getMessage(request, null)

    try {

      validate(params,message)

      // We're validated, store the user id and consumer key in the session
      val dialangSession = getDialangSession
      dialangSession.userId = params.get(BasicLTIConstants.USER_ID) match {
          case Some(s:String) => s
          case None => {
            logger.warn("No user id supplied in LTI launch")
            ""
          }
        }
      dialangSession.consumerKey = params.get("oauth_consumer_key") match {
          case Some(s:String) => s
          case None => {
            logger.warn("No consumer key supplied in LTI launch")
            ""
          }
        }

      // Grab the al,tl,skill and instant feedback custom parameters
      dialangSession.adminLanguage = params.get("custom_dialang_admin_language") match {
          case Some(s:String) => s
          case _ => params.get("launch_presentation_locale") match {
              case Some(s1:String) => db.getAdminLanguageForTwoLetterLocale(s1)
              case _ => ""
            }
        }

      dialangSession.testLanguage = params.getOrElse("custom_dialang_test_language", "")
      dialangSession.skill = params.getOrElse("custom_dialang_test_skill", "")

      dialangSession.instantFeedbackDisabled
        = params.get("custom_dialang_instant_feedback_disabled") match {
          case Some("true") => true
          case _ => false
        }

      if(logger.isDebugEnabled) {
        logger.debug("adminLanguage:" + dialangSession.adminLanguage)
        logger.debug("testLanguage:" + dialangSession.testLanguage)
        logger.debug("skill:" + dialangSession.skill)
        logger.debug("instantFeedbackDisabled:" + dialangSession.instantFeedbackDisabled)
      }

      if(dialangSession.adminLanguage == "") {
        saveDialangSession(dialangSession)
        contentType = "text/html"
        redirect("/dialang-content/als.html")
      } else {
        if(dialangSession.testLanguage == "" || dialangSession.skill == "") {
          saveDialangSession(dialangSession)
          contentType = "text/html"
          mustache("shell","state" -> "legend",
                              "al" -> dialangSession.adminLanguage,
                              "instantFeedbackDisabled" -> dialangSession.instantFeedbackDisabled)
        } else {
          dialangSession.sessionId = UUID.randomUUID.toString
          dialangSession.passId = UUID.randomUUID.toString
          dialangSession.ipAddress = request.remoteAddress
          saveDialangSession(dialangSession)

          // An admin languge, test language and skill have been specified as
          // launch parameters, so we can create a data capture session.
          // Usually, this happens after the TLS screen, in SetTLS.scala.
          dataCapture.createSessionAndPass(dialangSession)

          contentType = "text/html"
          mustache("shell","state" -> "vsptintro",
                              "al" -> dialangSession.adminLanguage,
                              "tl" -> dialangSession.testLanguage,
                              "skill" -> dialangSession.skill,
                              "instantFeedbackDisabled" -> dialangSession.instantFeedbackDisabled)
        }
      }
    } catch {
      case e:Exception => {
        logger.error("The LTI launch blew up.", e.getMessage)
      }
    }
	}

  @throws[Exception]
  private def validate(payload:Map[String,String], oam:OAuthMessage) {

    //check parameters
    val lti_message_type = payload.getOrElse(BasicLTIConstants.LTI_MESSAGE_TYPE, "")
    val lti_version = payload.getOrElse(BasicLTIConstants.LTI_VERSION, "")
    val oauth_consumer_key = payload.getOrElse("oauth_consumer_key", "")
    val resource_link_id = payload.getOrElse(BasicLTIConstants.RESOURCE_LINK_ID, "")
    val user_id = payload.getOrElse(BasicLTIConstants.USER_ID, "")
    val context_id = payload.getOrElse(BasicLTIConstants.CONTEXT_ID, "")

    if (lti_message_type != "basic-lti-launch-request") {
      println(lti_message_type)
      throw new Exception("launch.invalid")
    }

    if (lti_version != "LTI-1p0") {
      throw new Exception( "launch.invalid")
    }

    if (oauth_consumer_key == "") {
      throw new Exception( "launch.missing oauth_consumer_key")
    }

    if(resource_link_id == "") {
      throw new Exception( "launch.missing resource_link_id")
    }

    if(user_id == "") {
      throw new Exception( "launch.missing user_id")
    }

    // Lookup the secret
    val oauth_secret = db.getSecret(oauth_consumer_key) match {
        case Some(s:String) => s
        case None => throw new Exception( "launch.key.notfound: '" + oauth_consumer_key + "'")
      }

    val oav = new SimpleOAuthValidator
    val cons = new OAuthConsumer("about:blank#OAuth+CallBack+NotUsed", oauth_consumer_key,oauth_secret, null)
    val acc = new OAuthAccessor(cons)

    var baseString:String = null
    try {
      baseString = OAuthSignatureMethod.getBaseString(oam)
    } catch {
      case e:Exception => {
        logger.error(e.getLocalizedMessage(), e)
      }
    }

    try {
      oav.validateMessage(oam, acc)
    } catch {
      case e:Exception => {
        logger.warn("Provider failed to validate message")
        logger.warn(e.getLocalizedMessage, e)
        if (baseString != null) {
          logger.warn("BASE STRING: " + baseString)
        }
        throw new Exception( "launch.no.validate", e)
      }
    }
  }
}
