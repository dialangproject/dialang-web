package org.dialang.web.servlets

import net.oauth._
import net.oauth.server.OAuthServlet
import net.oauth.signature.OAuthSignatureMethod

import org.dialang.web.model.{DialangSession, InstructorSession, TES}
import org.dialang.web.util.{HashUtils, ValidityChecks}

import scala.collection.JavaConversions._

import org.slf4j.LoggerFactory

import scalaj.http.{Http,HttpOptions}
import org.json4s._
import org.json4s.native.JsonMethods._

import org.scalatra.scalate.ScalateSupport

import java.io.InputStreamReader
import java.util.{Date, UUID}
import javax.servlet.{ServletConfig,ServletContext}

class LTILaunch extends DialangServlet with ScalateSupport {

  private val logger = LoggerFactory.getLogger(classOf[LTILaunch])

  private val AdminLanguageKey = "custom_admin_language"
  private val TestLanguageKey = "custom_test_language"
  private val TestSkillKey = "custom_test_skill"
  private val DisallowInstantFeedbackKey = "custom_disallow_instant_feedback"
  private val HideVSPTKey = "custom_hide_vspt"
  private val HideVSPTResultKey = "custom_hide_vspt_result"
  private val HideSAKey = "custom_hide_sa"
  private val HideTestKey = "custom_hide_test"
  private val TestDifficultyKey = "custom__test_difficulty"
  private val HideFeedbackMenuKey = "custom_hide_feedback_menu"
  private val TESURLKey = "custom_tes_url"

  private lazy val launchUrl = {
      config.getInitParameter("launchUrl") match {
          case "" => null
          case s: String => s
        }
    }

  post("/") {

    logger.debug("LTILaunch.post")

    if (logger.isDebugEnabled) logger.debug("launchUrl:" + launchUrl);

    val message: OAuthMessage = OAuthServlet.getMessage(request, launchUrl)

    try {
      validate(params, message)

      params.get(BasicLTIConstants.ROLES) match {
        case Some(roles: String) => {
          if (roles.contains("Instructor") || roles.contains("Teacher")) {
            val al = getLTILaunchLocale(params)
            val oauthConsumerKey = params.get(OAuth.OAUTH_CONSUMER_KEY).get
            val instructorSession = new InstructorSession(oauthConsumerKey, al)
            saveInstructorSession(instructorSession)
            contentType = "text/html"
            mustache("shell","state" -> "instructormenu", "al" -> getLTILaunchLocale(params))
          } else {
            launchNonInstructor(params)
          }
        }
        case _ => launchNonInstructor(params)
      }
    } catch {
      case e: Exception => {
        logger.error("The LTI launch blew up.", e)
        contentType = "text/html"
        <html>
          <body>
            <h1>Launch Error</h1>
            <p>{e.getMessage}</p>
          </body>
        </html>
      }
    }
  }

  private def launchNonInstructor(params: Map[String, String]) = {

    // We're validated, store the user id and consumer key in the session
    val dialangSession = getDialangSession

    // Each LTI launch is a new session, so clear it.
    dialangSession.clear()

    dialangSession.started = ((new Date).getTime) / 1000L

    // validate checks that these are present
    dialangSession.userId = params.get(BasicLTIConstants.USER_ID).get
    dialangSession.consumerKey = params.get(OAuth.OAUTH_CONSUMER_KEY).get

    if (logger.isDebugEnabled) {
      logger.debug("userId:" + dialangSession.userId)
    }
    
    if (logger.isDebugEnabled) {
      logger.debug(dialangSession.tes.toString)
    }

    dialangSession.tes = getOrBuildTestExecutionScript(params, dialangSession)

    if (dialangSession.tes.al == "") {
      // Still no admin language, launch the als screen.
      saveDialangSession(dialangSession)
      contentType = "text/html"
      redirect("getals")
    } else {
      // An admin language has been specified
      if (ValidityChecks.adminLanguageExists(dialangSession.tes.al)) {
        if (dialangSession.tes.tl == "" || dialangSession.tes.skill == "") {
          // ... but the test language and skill have not
          saveDialangSession(dialangSession)
          renderPostALSView(dialangSession.tes, "legend")
        } else {
          if (ValidityChecks.testLanguageExists(dialangSession.tes.tl) && ValidityChecks.skillExists(dialangSession.tes.skill)) {
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
                                "al" -> dialangSession.tes.al,
                                "tl" -> dialangSession.tes.tl,
                                "skill" -> dialangSession.tes.skill,
                                "hideALS" -> true,
                                "hideTLS" -> true,
                                "hideVSPT" -> dialangSession.tes.hideVSPT,
                                "hideVSPTResult" -> dialangSession.tes.hideVSPTResult,
                                "hideSA" -> dialangSession.tes.hideSA,
                                "hideTest" -> dialangSession.tes.hideTest,
                                "hideFeedbackMenu" -> dialangSession.tes.hideFeedbackMenu,
                                "disallowInstantFeedback" -> dialangSession.tes.disallowInstantFeedback)
          } else {
            logger.warn("Invalid test language '" + dialangSession.tes.tl + "' supplied. Rendering the TLS view ...")
            saveDialangSession(dialangSession)
            renderPostALSView(dialangSession.tes, "tls")
          }
        }
      } else {
        logger.warn("Invalid admin language '" + dialangSession.tes.al + "' supplied. Rendering the ALS view ...")
        contentType = "text/html"
        redirect("getals")
      }
    }
  }

  private def getOrBuildTestExecutionScript(params: Map[String, String], dialangSession: DialangSession): TES = {

    val tesUrl = params.getOrElse(TESURLKey, "")

    if (tesUrl != "") {
      // A Test Execution Script callback has been supplied

      val oauth_secret = db.getSecret(dialangSession.consumerKey).get

      val hash = HashUtils.getHash(dialangSession.userId + dialangSession.consumerKey, oauth_secret)

      if (logger.isDebugEnabled) logger.debug("hash:" + hash)

      Http(tesUrl)
        .option(HttpOptions.allowUnsafeSSL)
        .option(HttpOptions.connTimeout(1000))
        .option(HttpOptions.readTimeout(5000))
        .params("user" -> dialangSession.userId, "hash" -> hash){inputStream => {
            implicit val formats = DefaultFormats
            val tesJson = parse(new InputStreamReader(inputStream))
            //dialangSession.tes = tesJson.extract[TES]
            tesJson.extract[TES]
          }
        }
    } else {
      // If no admin language has been specified by custom LTI launch
      // parameters, try and get the LTI launch locale.
      val al = params.get(AdminLanguageKey) match {
          case Some(s: String) => s
          case _ => getLTILaunchLocale(params)
        }
      val tl = params.getOrElse(TestLanguageKey, "")
      val skill = params.getOrElse(TestSkillKey, "")
      val hideVSPT = params.get(HideVSPTKey) match {
          case Some("true") => true
          case _ => false
        }
      val hideVSPTResult = params.get(HideVSPTResultKey) match {
          case Some("true") => true
          case _ => false
        }
      val hideSA = params.get(HideSAKey) match {
          case Some("true") => true
          case _ => false
        }
      val hideTest = params.get(HideTestKey) match {
          case Some("true") => true
          case _ => false
        }
      val testDifficulty = params.getOrElse(TestDifficultyKey, "")
      val hideFeedbackMenu = params.get(HideFeedbackMenuKey) match {
          case Some("true") => true
          case _ => false
        }
      val disallowInstantFeedback = params.get(DisallowInstantFeedbackKey) match {
          case Some("true") => true
          case _ => false
        }

      TES("", al, tl, skill, hideVSPT, hideVSPTResult, hideSA, hideTest, testDifficulty, hideFeedbackMenu, disallowInstantFeedback, "")
    }
  }

  @throws[Exception]
  private def validate(payload: Map[String, String], oam: OAuthMessage) {

    //check parameters
    val lti_message_type = payload.getOrElse(BasicLTIConstants.LTI_MESSAGE_TYPE, "")

    if (lti_message_type != "basic-lti-launch-request") {
      throw new Exception("launch.invalid. Invalid lti_message_type: " + lti_message_type)
    }

    val lti_version = payload.get(BasicLTIConstants.LTI_VERSION) match {
        case Some(s: String) => s
        case None => {
          throw new Exception("launch.invalid. No lti_version.")
        }
      }

    val oauth_consumer_key = payload.get(OAuth.OAUTH_CONSUMER_KEY) match {
        case Some(s: String) => s
        case None => {
          throw new Exception("launch.invalid. Missing oauth_consumer_key.")
        }
      }

    val resource_link_id = payload.get(BasicLTIConstants.RESOURCE_LINK_ID) match {
        case Some(s: String) => s
        case None => {
          throw new Exception("launch.invalid Missing resource_link_id.")
        }
      }

    val user_id = payload.get(BasicLTIConstants.USER_ID) match {
        case Some(s: String) => s
        case None => {
          throw new Exception("launch.invalid. Missing user_id.")
        }
      }

    // Lookup the secret
    val oauth_secret = db.getSecret(oauth_consumer_key) match {
      case Some(s: String) => s
      case None => {
        throw new Exception("launch.invalid. No secret for consumer key: '" + oauth_consumer_key)
      }
    }

    val cons = new OAuthConsumer("about:blank#OAuth+CallBack+NotUsed", oauth_consumer_key, oauth_secret, null)
    val acc = new OAuthAccessor(cons)
    val oav = new SimpleOAuthValidator

    try {
      oav.validateMessage(oam, acc)
    } catch {
      case e: Exception => {
        throw new Exception( "launch.no.validate", e)
      }
    }
  }

  private def renderPostALSView(tes: TES, state: String) = {

    if (logger.isDebugEnabled) {
      logger.debug("Showing '" + state + "' view ...")
    }

    contentType = "text/html"
    mustache("shell", "state" -> state,
                        "al" -> tes.al,
                        "hideALS" -> true,
                        "hideVSPT" -> tes.hideVSPT,
                        "hideVSPTResult" -> tes.hideVSPTResult,
                        "hideSA" -> tes.hideSA,
                        "hideTest" -> tes.hideTest,
                        "hideFeedbackMenu" -> tes.hideFeedbackMenu,
                        "disallowInstantFeedback" -> tes.disallowInstantFeedback)
  }

  private def getLTILaunchLocale(params: Map[String, String]) = {

    params.get(BasicLTIConstants.LAUNCH_PRESENTATION_LOCALE) match {
      case Some(s: String) => db.ltiLocaleLookup.getOrElse(s, "")
      case _ => ""
    }
  }
}
