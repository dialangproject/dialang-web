package org.dialang.servlets

import java.io.{IOException,PrintWriter}
import java.net.{URI,URISyntaxException,URLEncoder}

import javax.servlet.{ServletConfig,ServletException}
import javax.servlet.http.{HttpServlet,HttpServletRequest,HttpServletResponse}

import net.oauth._
import net.oauth.server.OAuthServlet
import net.oauth.signature.OAuthSignatureMethod

//import org.apache.commons.logging.{Log,LogFactory}

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

class LTILaunch extends DialangServlet {

  //private M_log = LogFactory.getLog(LTIServlet.class)

  @throws[ServletException]
  @throws[IOException]
	override def doGet(request:HttpServletRequest , response:HttpServletResponse ) {
	  doPost(request, response)
	}

  @throws[ServletException]
  @throws[IOException]
	override def doPost(request:HttpServletRequest, response:HttpServletResponse) {

    println("doPost")

		val payload = getPayloadAsMap(request)
    val message = OAuthServlet.getMessage(request, null)

    try {
      validate(payload,message)

      // We're validated, store the user id
      val user_id = payload.get(BasicLTIConstants.USER_ID).get

      val cookie = getUpdatedCookie(request,Map("userId" -> user_id))

      response.addCookie(cookie)

      response.setStatus(HttpServletResponse.SC_OK)
      response.setContentType("text/html")
      response.sendRedirect(staticContentRoot + "als.html")
    } catch {
      case e:Exception => {
        println(e.getMessage)
      }
    }
	}

  private def getPayloadAsMap(request:HttpServletRequest ) = {

    val payload = new HashMap[String,String]
    request.getParameterMap.foreach(t => {
      val value = t._2.asInstanceOf[Array[String]](0)
      payload += ((t._1.asInstanceOf[String],value))
    })
    payload.toMap
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
    val oauth_secret = "secret"
    if (oauth_secret == null) {
      throw new Exception( "launch.key.notfound")
    }
          
    val oav = new SimpleOAuthValidator
    val cons = new OAuthConsumer("about:blank#OAuth+CallBack+NotUsed", oauth_consumer_key,oauth_secret, null)
    val acc = new OAuthAccessor(cons)

    var base_string:String = null
    try {
      base_string = OAuthSignatureMethod.getBaseString(oam)
    } catch {
      case e:Exception => {
        //M_log.error(e.getLocalizedMessage(), e);
      }
    }

    try {
      oav.validateMessage(oam, acc)
    } catch {
      case e:Exception => {
        //M_log.warn("Provider failed to validate message");
        //M_log.warn(e.getLocalizedMessage(), e);
        if (base_string != null) {
          //M_log.warn(base_string);
        }
        throw new Exception( "launch.no.validate", e)
      }
    }
  }
}
