package org.dialang.web.servlets

import javax.servlet.http.Cookie

import org.dialang.web.model.DialangSession
import org.dialang.web.datacapture.DataCapture

import java.net.{URLEncoder,URLDecoder}

import scala.collection.mutable.HashMap

import org.scalatra.ScalatraServlet

class DialangServlet extends ScalatraServlet {

  protected lazy val dataCapture = new DataCapture

  protected def getDialangSession = {
    session.get("dialangSession") match {
      case Some(d:DialangSession) => d
      case _ => new DialangSession
    }
  }

  protected def saveDialangSession(dialangSession:DialangSession) {
    session += ("dialangSession" -> dialangSession)
  }

  protected def getUpdatedCookie(state: Map[String,String],ignoreCurrent: Boolean = false): Cookie = {

    if(ignoreCurrent) {
      val cookieValue = state.foldLeft("")((b,a) => b + a._1 + "=" + a._2 + "|").dropRight(1)
      val cookie = new Cookie("DIALANG",URLEncoder.encode(cookieValue,"UTF-8"))
      cookie.setPath("/")
      cookie
    } else {
      val newMap = getCookieMap ++ state
      val cookieValue = newMap.foldLeft("")((b,a) => b + a._1 + "=" + a._2 + "|").dropRight(1)
      val cookie = new Cookie("DIALANG",URLEncoder.encode(cookieValue,"UTF-8"))
      cookie.setPath("/")
      cookie
    }
  }

  private def getCookieMap : Map[String,String] = {

    var dialangCookie: Cookie = null

    val cookies = request.getCookies
    if(cookies != null) {
      cookies.foreach(cookie => {
        if(cookie.getName == "DIALANG") {
          dialangCookie = cookie
        }
      })
    }

    if(dialangCookie == null) {
      Map()
    } else {
      val mutableMap = new HashMap[String,String]
      val value = URLDecoder.decode(dialangCookie.getValue,"UTF-8")
      value.split("\\|").foreach(pair => {
        if(pair.length > 0) {
          val keyValue = pair.split("=")
          val key = keyValue(0)
          val value = keyValue(1)
          mutableMap += (key -> value)
        }
      })
      mutableMap.toMap
    }
  }
}