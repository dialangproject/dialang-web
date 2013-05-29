package org.dialang.web.servlets

import org.dialang.web.model.DialangSession
import org.dialang.common.model.Item

import scala.collection.JavaConversions._

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.scalatra.ScalatraServlet

import org.dialang.web.db.DB

import org.json4s.{DefaultFormats, Formats}

// JSON handling support from Scalatra
import org.scalatra.json._

class Dialang extends ScalatraServlet with JacksonJsonSupport {

  protected implicit val jsonFormats: Formats = DefaultFormats

  private val logger = LoggerFactory.getLogger(classOf[Dialang])

  private val db = DB

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }

  post("/setals/:al") {
    val al = params("al")

    val dialangSession = getSession
    dialangSession.adminLanguage = al
    saveSession(dialangSession)

    // This updates and gets the updated cookie so we can set it on the response.
    val map = Map("al" -> al)
    val cookie = getUpdatedCookie(req,map,true)

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.addCookie(cookie)
    resp.setContentType("text/html")
    resp.sendRedirect("/legend/" + al + ".html")
  }

  def getSession = {
    session.get("dialangSession") match {
      case Some(s:DialangSession) => s
      case _ => new DialangSession
    }
  }

  def saveSession(session:DialangSession) {
    session.set("dialangSession",session)
  }
}
