package org.dialang.web.servlets

import net.oauth._
import net.oauth.server.OAuthServlet

import org.apache.commons.codec.digest.HmacUtils
import org.apache.commons.codec.digest.HmacAlgorithms._

import org.dialang.common.model.{DialangSession, TES}
import org.dialang.web.model.InstructorSession
import org.dialang.web.util.ValidityChecks

import scala.collection.JavaConversions._

import org.scalatra.{BadRequest, Forbidden}
import org.scalatra.scalate.ScalateSupport

import java.io.FileInputStream
import java.util.{Date, Properties, UUID}

class LTILaunch extends DialangServlet with ScalateSupport {

  private val AdminLanguageKey = "custom_admin_language"
  private val TestLanguageKey = "custom_test_language"
  private val TestSkillKey = "custom_test_skill"
  private val DisallowInstantFeedbackKey = "custom_disallow_instant_feedback"
  private val HideVSPTKey = "custom_hide_vspt"
  private val HideVSPTResultKey = "custom_hide_vspt_result"
  private val HideSAKey = "custom_hide_sa"
  private val HideTestKey = "custom_hide_test"
  private val TestDifficultyKey = "custom_test_difficulty"
  private val HideFeedbackMenuKey = "custom_hide_feedback_menu"
  private val ResultUrl = "custom_result_url"

  private lazy val launchUrl = {
      config.getInitParameter("launchUrl") match {
          case "" => null
          case s: String => s
        }
    }

  private lazy val configProperties = {
      val props = new Properties
      config.getInitParameter("configFile") match {
          case null => props
          case "" => props
          case s: String => {
            val props = new Properties
            try {
              props.load(new FileInputStream(s))
            } catch {
              case e: Exception => {
                logger.error("Failed to load the config properties from " + s, e)
              }
            }
            props
          }
        }
    }

  get("/") {

    val userId = params.get("userId")
    val consumerKey = params.get("consumerKey")
    val hash = params.get("hash")

    if (userId.isEmpty || consumerKey.isEmpty || hash.isEmpty) {
      BadRequest("You must provide a userId, consumerKey and hash")
    } else {
      logger.debug("userId: " + userId)
      logger.debug("consumerKey: " + consumerKey)
      logger.debug("hash: " + hash)

      val secret = db.getSecret(consumerKey.get)

      if (secret.isEmpty) {
        BadRequest("consumerKey not recognised")
      } else {
        val testHash = new HmacUtils(HMAC_SHA_1, secret.get).hmacHex(consumerKey.get + userId.get);
        if (testHash != hash.get) {
          logger.debug("Hashes don't match.")
          logger.debug("Theirs: " + hash)
          logger.debug("Ours: " + testHash)
          Forbidden("Hashes don't match")
        } else {
          launchNonInstructor(Map(BasicLTIConstants.USER_ID -> userId.get, OAuth.OAUTH_CONSUMER_KEY -> consumerKey.get))
        }
      }
    }
  }

  post("/") {

    logger.debug("LTILaunch.post")

    logger.debug("launchUrl: " + launchUrl)

    val message: OAuthMessage = OAuthServlet.getMessage(request, launchUrl)

    try {
      validate(params, message)

      params.get(BasicLTIConstants.ROLES) match {
        case Some(roles: String) => {
          val instructorRoles = {
              configProperties.getProperty("instructorRoles", "") match {
                case "" => List("Instructor", "Teacher")
                case s: String => s.split(",").toList
              }
            }

          if (roles.split(",").toList.intersect(instructorRoles).length > 0) {
            val oauthConsumerKey = params.get(OAuth.OAUTH_CONSUMER_KEY).get
            val al = getLTILaunchLocale(params)
            val resourceLinkId = params.get(BasicLTIConstants.RESOURCE_LINK_ID).get
            val instructorSession = new InstructorSession(oauthConsumerKey, resourceLinkId, al)
            saveInstructorSession(instructorSession)
            contentType = "text/html"
            mustache("shell", "state" -> "instructormenu", "al" -> getLTILaunchLocale(params))
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
    dialangSession.clearSession()

    dialangSession.started = new Date

    dialangSession.userId = params.get(BasicLTIConstants.USER_ID).get
    dialangSession.firstName = params.getOrElse(BasicLTIConstants.LIS_PERSON_NAME_GIVEN, "")
    dialangSession.lastName = params.getOrElse(BasicLTIConstants.LIS_PERSON_NAME_FAMILY, "")
    dialangSession.consumerKey = params.get(OAuth.OAUTH_CONSUMER_KEY).get
    dialangSession.resourceLinkId = params.getOrElse(BasicLTIConstants.RESOURCE_LINK_ID, "")
    dialangSession.resourceLinkTitle = params.getOrElse(BasicLTIConstants.RESOURCE_LINK_TITLE, "")

    dialangSession.resultUrl = params.getOrElse(ResultUrl, "")

    dialangSession.tes = getOrBuildTestExecutionScript(params, dialangSession)

    logger.debug("userId: " + dialangSession.userId)
    logger.debug("firstName: " + dialangSession.firstName)
    logger.debug("lastName: " + dialangSession.lastName)
    logger.debug("consumerKey: " + dialangSession.consumerKey)
    logger.debug("resourceLinkId: " + dialangSession.resourceLinkId)
    logger.debug("resultUrl: " + dialangSession.resultUrl)
    logger.debug("resourceLinkTitle: " + dialangSession.resourceLinkTitle)
    logger.debug(dialangSession.tes.toString)

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
            dialangSession.browserLocale = request.locale.toString
            dialangSession.browserReferrer = request.header("referer").getOrElse("")
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

    val al = params.getOrElse(AdminLanguageKey, getLTILaunchLocale(params))
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

    logger.debug("Showing '" + state + "' view ...")

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
      case Some(s: String) => db.ltiLocaleLookup.getOrElse(s, "eng_gb")
      case _ => "eng_gb"
    }
  }
}
