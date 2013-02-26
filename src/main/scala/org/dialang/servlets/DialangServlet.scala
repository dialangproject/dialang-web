package org.dialang.servlets

import javax.servlet.http._

import java.net.{URLEncoder,URLDecoder}

import scala.collection.mutable.HashMap

class DialangServlet extends HttpServlet{

  def getUpdatedCookie(req: HttpServletRequest, state: Map[String,String],ignoreCurrent: Boolean = false): Cookie = {

    if(ignoreCurrent) {
      val cookieValue = state.foldLeft("")((b,a) => b + a._1 + "=" + a._2 + "|").dropRight(1)
      new Cookie("DIALANG",URLEncoder.encode(cookieValue,"UTF-8"))
    } else {
      val newMap = getCookieMap(req) ++ state
      val cookieValue = newMap.foldLeft("")((b,a) => b + a._1 + "=" + a._2 + "|").dropRight(1)
      new Cookie("DIALANG",URLEncoder.encode(cookieValue,"UTF-8"))
    }
  }

  def getCookieMap(req: HttpServletRequest) : Map[String,String] = {

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
