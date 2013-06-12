package org.dialang.web.servlets

import net.oauth._
import net.oauth.server.OAuthServlet
import net.oauth.signature.OAuthSignatureMethod

import org.dialang.web.db.DB

import scala.collection.JavaConversions._

import org.slf4j.LoggerFactory;

import org.scalatra.scalate.ScalateSupport

class LTILaunch extends DialangServlet with ScalateSupport {

  private val logger = LoggerFactory.getLogger(classOf[LTILaunch])

  val db = DB

	post("/") {

    /*
		val payload = params.map(t => {
        val value = t._2
        ((t._1.asInstanceOf[String],t._2))
      })
    */

    val message = OAuthServlet.getMessage(request, null)

    try {

      //params.foreach(t => { println(t._1 + " -> " + t._2) })

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

      // Grab the al,tl and skill
      dialangSession.adminLanguage = params.getOrElse("custom_dialang_admin_language","")
      dialangSession.testLanguage = params.getOrElse("custom_dialang_test_language","")
      dialangSession.skill = params.getOrElse("custom_dialang_test_skill","")

      logger.debug("al:" + dialangSession.adminLanguage)

      saveDialangSession(dialangSession)

      if(dialangSession.adminLanguage == "") {
        contentType = "text/html"
        redirect("/dialang-content/als.html")
      } else {

        if(dialangSession.testLanguage == "" || dialangSession.skill == "") {
          contentType = "text/html"
          mustache("shell","state" -> "legend","al" -> dialangSession.adminLanguage)
        } else {
          contentType = "text/html"
          mustache("shell","state" -> "vsptintro",
                      "al" -> dialangSession.adminLanguage,
                      "tl" -> dialangSession.testLanguage,
                      "skill" -> dialangSession.skill)
        }
      }
    } catch {
      case e:Exception => {
        println(e.getMessage)
      }
    }
	}

  @throws[Exception]
  private def validate(payload:Map[String,String],oam:OAuthMessage) {

    //check parameters
    val lti_message_type = payload.getOrElse(BasicLTIConstants.LTI_MESSAGE_TYPE,"")
    val lti_version = payload.getOrElse(BasicLTIConstants.LTI_VERSION,"")
    val oauth_consumer_key = payload.getOrElse("oauth_consumer_key","")
    val resource_link_id = payload.getOrElse(BasicLTIConstants.RESOURCE_LINK_ID,"")
    val user_id = payload.getOrElse(BasicLTIConstants.USER_ID,"")
    val context_id = payload.getOrElse(BasicLTIConstants.CONTEXT_ID,"")

    if(lti_message_type != "basic-lti-launch-request") {
      throw new Exception("launch.invalid")
    }

    if(lti_version != "LTI-1p0") {
      throw new Exception( "launch.invalid")
    }

    if(oauth_consumer_key == "") {
      throw new Exception( "launch.missing")
    }

    if(resource_link_id == "") {
      throw new Exception( "launch.missing")
    }

    if(user_id == "") {
      throw new Exception( "launch.missing")
    }

    // Lookup the secret
    val oauth_secret = db.getSecret(oauth_consumer_key) match {
        case Some(s:String) => s
        case None => throw new Exception( "launch.key.notfound")
      }
          
    val oav = new SimpleOAuthValidator
    val cons = new OAuthConsumer("about:blank#OAuth+CallBack+NotUsed", oauth_consumer_key,oauth_secret, null)
    val acc = new OAuthAccessor(cons)

    var base_string:String = null
    try {
      base_string = OAuthSignatureMethod.getBaseString(oam)
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
        if (base_string != null) {
          logger.warn(base_string)
        }
        throw new Exception( "launch.no.validate", e)
      }
    }
  }
}
