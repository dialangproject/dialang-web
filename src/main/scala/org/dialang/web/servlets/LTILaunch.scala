package org.dialang.web.servlets

import net.oauth._
import net.oauth.server.OAuthServlet
import net.oauth.signature.OAuthSignatureMethod

import org.dialang.web.db.DBFactory
import org.dialang.web.model.TES
import org.dialang.web.util.HashUtils

import scala.collection.JavaConversions._

import org.slf4j.LoggerFactory

import scalaj.http.{Http,HttpOptions}
import org.json4s._
import org.json4s.native.JsonMethods._

import org.scalatra.scalate.ScalateSupport

import java.io.InputStreamReader
import java.util.UUID

class LTILaunch extends DialangServlet with ScalateSupport {

  private val logger = LoggerFactory.getLogger(classOf[LTILaunch])

  private val DialangAdminLanguageKey = "custom_dialang_admin_language"
  private val DialangTestLanguageKey = "custom_dialang_test_language"
  private val DialangTestSkillKey = "custom_dialang_test_skill"
  private val DialangDisallowInstantFeedbackKey = "custom_dialang_disallow_instant_feedback"
  private val DialangTESURLKey = "custom_dialang_tes_url"

	post("/") {

    logger.debug("LTI Launch")

    val message = OAuthServlet.getMessage(request, null)

    try {
      validate(params, message)

      // We're validated, store the user id and consumer key in the session
      val dialangSession = getDialangSession

      // Each LTI launch is a new session, so clear it.
      dialangSession.clear()

      dialangSession.userId = params.get(BasicLTIConstants.USER_ID).get
      dialangSession.consumerKey = params.get("oauth_consumer_key").get

      if (logger.isDebugEnabled) {
        logger.debug("userId:" + dialangSession.userId)
      }

      val tesUrl = params.getOrElse(DialangTESURLKey, "")

      if (tesUrl != "") {
        // A Test Execution Script callback has been supplied
        if (logger.isDebugEnabled) {
          logger.debug("tesUrl:" + tesUrl)
        }

        val oauth_secret = db.getSecret(dialangSession.consumerKey).get

        val hash = HashUtils.getHash(dialangSession.userId + dialangSession.consumerKey, oauth_secret)

        if (logger.isDebugEnabled) logger.debug("hash:" + hash)

        Http(tesUrl).option(HttpOptions.allowUnsafeSSL)
          .params("user" -> dialangSession.userId, "hash" -> hash){inputStream => {
            implicit val formats = DefaultFormats
            val tesJson = parse(new InputStreamReader(inputStream))
            val tes = tesJson.extract[TES]
            if (logger.isDebugEnabled) logger.debug("testCompleteUrl: " + tes.testCompleteUrl)
            dialangSession.tes = tes
            dialangSession.adminLanguage = tes.al
            dialangSession.testLanguage = tes.tl
            dialangSession.skill = tes.skill
            dialangSession.disallowInstantFeedback = tes.disallowInstantFeedback
          }
        }
      } else {
        // Grab the al,tl,skill and instant feedback custom parameters
        dialangSession.adminLanguage = params.get(DialangAdminLanguageKey) match {
          case Some(s:String) => s
          case _ => params.get(BasicLTIConstants.LAUNCH_PRESENTATION_LOCALE ) match {
            case Some(s1:String) => db.getAdminLanguageForTwoLetterLocale(s1)
            case _ => ""
          }
        }

        dialangSession.testLanguage = params.getOrElse(DialangTestLanguageKey, "")
        dialangSession.skill = params.getOrElse(DialangTestSkillKey, "")

        dialangSession.disallowInstantFeedback = params.get(DialangDisallowInstantFeedbackKey) match {
            case Some("true") => true
            case _ => false
        }
      }

      if (logger.isDebugEnabled) {
        logger.debug("userId:" + dialangSession.userId)
        logger.debug("adminLanguage:" + dialangSession.adminLanguage)
        logger.debug("testLanguage:" + dialangSession.testLanguage)
        logger.debug("skill:" + dialangSession.skill)
      }

      if (dialangSession.adminLanguage == "") {
        saveDialangSession(dialangSession)
        contentType = "text/html"
        redirect("getals")
      } else {
        // An admin language has been specified
        //dialangSession.hideALS = false
        if (dialangSession.testLanguage == "" || dialangSession.skill == "") {
          // ... but the test language and skill have not
          saveDialangSession(dialangSession)
          contentType = "text/html"
          mustache("shell", "state" -> "legend",
                              "al" -> dialangSession.adminLanguage,
                              "hideALS" -> true,
                              "hideVSPT" -> dialangSession.tes.hideVSPT,
                              "hideVSPTResult" -> dialangSession.tes.hideVSPTResult,
                              "hideSA" -> dialangSession.tes.hideSA,
                              "hideTest" -> dialangSession.tes.hideTest,
                              "hideFeedbackMenu" -> dialangSession.tes.hideFeedbackMenu,
                              "disallowInstantFeedback" -> dialangSession.disallowInstantFeedback)
        } else {
          //dialangSession.showTLS = false
          dialangSession.sessionId = UUID.randomUUID.toString
          dialangSession.passId = UUID.randomUUID.toString
          dialangSession.ipAddress = request.remoteAddress
          saveDialangSession(dialangSession)

          // An admin languge, test language and skill have been specified as
          // launch parameters, so we can create a data capture session.
          // Usually, this happens after the TLS screen, in SetTLS.scala.
          dataCapture.createSessionAndPass(dialangSession)

          val initialState = {
              if (!dialangSession.tes.hideVSPT) "vsptintro"
              else if (!dialangSession.tes.hideSA) "saintro"
              else if (!dialangSession.tes.hideTest) "endoftest"
              else "testintro"
            }

          contentType = "text/html"
          mustache("shell","state" -> initialState,
                              "al" -> dialangSession.adminLanguage,
                              "tl" -> dialangSession.testLanguage,
                              "skill" -> dialangSession.skill,
                              "hideALS" -> true,
                              "hideTLS" -> true,
                              "hideVSPT" -> dialangSession.tes.hideVSPT,
                              "hideVSPTResult" -> dialangSession.tes.hideVSPTResult,
                              "hideSA" -> dialangSession.tes.hideSA,
                              "hideTest" -> dialangSession.tes.hideTest,
                              "hideFeedbackMenu" -> dialangSession.tes.hideFeedbackMenu,
                              "disallowInstantFeedback" -> dialangSession.disallowInstantFeedback)
        }
      }
    } catch {
      case e:Exception => {
        logger.error("The LTI launch blew up.", e)
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
      logger.error("Invalid lti_message_type: " + lti_message_type)
      throw new Exception("launch.invalid")
    }

    if (lti_version != "LTI-1p0") {
      logger.error("Invalid lti_version: " + lti_version)
      throw new Exception( "launch.invalid")
    }

    if (oauth_consumer_key == "") {
      logger.error("Missing outh_consumer_key")
      throw new Exception( "launch.missing oauth_consumer_key")
    }

    if (resource_link_id == "") {
      logger.error("Missing resource_link_id")
      throw new Exception( "launch.missing resource_link_id")
    }

    if (user_id == "") {
      logger.error("Missing user_id")
      throw new Exception( "launch.missing user_id")
    }

    // Lookup the secret
    val oauth_secret = db.getSecret(oauth_consumer_key) match {
      case Some(s:String) => s
      case None => {
        logger.error("launch.key.notfound: " + oauth_consumer_key)
        throw new Exception("launch.key.notfound: '" + oauth_consumer_key)
      }
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
        logger.error("Provider failed to validate message")
        logger.error(e.getLocalizedMessage, e)
        if (baseString != null) {
          logger.info("BASE STRING: " + baseString)
        }
        throw new Exception( "launch.no.validate", e)
      }
    }
  }
}
