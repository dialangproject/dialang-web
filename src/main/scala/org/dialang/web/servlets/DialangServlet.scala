package org.dialang.web.servlets

import javax.servlet.http._

import org.dialang.web.datacapture.DataCapture
import org.dialang.web.model.DialangSession

import java.net.{URLEncoder,URLDecoder}

import scala.collection.mutable.HashMap

class DialangServlet extends HttpServlet {

  protected lazy val dataCapture = new DataCapture

  protected def getDialangSession(req: HttpServletRequest) = {
    req.getSession.getAttribute("dialangSession") match {
      case d:DialangSession => d
      case _ => new DialangSession
    }
  }

  protected def saveDialangSession(dialangSession:DialangSession,req: HttpServletRequest) {
    req.getSession.setAttribute("dialangSession",dialangSession)
  }

  protected def getUpdatedCookie(req: HttpServletRequest, state: Map[String,String],ignoreCurrent: Boolean = false): Cookie = {

    if(ignoreCurrent) {
      val cookieValue = state.foldLeft("")((b,a) => b + a._1 + "=" + a._2 + "|").dropRight(1)
      val cookie = new Cookie("DIALANG",URLEncoder.encode(cookieValue,"UTF-8"))
      cookie.setPath("/")
      cookie
    } else {
      val newMap = getCookieMap(req) ++ state
      val cookieValue = newMap.foldLeft("")((b,a) => b + a._1 + "=" + a._2 + "|").dropRight(1)
      val cookie = new Cookie("DIALANG",URLEncoder.encode(cookieValue,"UTF-8"))
      cookie.setPath("/")
      cookie
    }
  }

  protected def addToSession(req:HttpServletRequest, map:Map[String,String]) {

    val session = req.getSession
    map.foreach(t => session.setAttribute(t._1,t._2))
  }

  private def getCookieMap(req: HttpServletRequest) : Map[String,String] = {

    var dialangCookie: Cookie = null

    val cookies = req.getCookies
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
