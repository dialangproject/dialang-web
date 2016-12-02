package org.dialang.web.servlets

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import org.dialang.common.model.DialangSession
import org.dialang.scoring.ScoringMethods

import org.json4s.{DefaultFormats, Formats}

import org.scalatra.json._

import org.slf4j.LoggerFactory;

class ScoreSA extends DialangServlet with JacksonJsonSupport {

  private val logger = LoggerFactory.getLogger(getClass)

  private val scoringMethods = new ScoringMethods

  protected implicit val jsonFormats: Formats = DefaultFormats

  post("/") {

    val responses = {
        val tmp = new HashMap[String,Boolean]
        params.foreach( t => {
          val name = t._1.asInstanceOf[String]
          if(name.startsWith("statement:")) {
            val wid = name.split(":")(1)

            val answer = t._2 match {
                case "yes" => true
                case "no" => false
              }

            tmp += (wid -> answer)
          }
        })
        tmp.toMap
      }

    val dialangSession = getDialangSession

    if(dialangSession.tes.tl == "" || dialangSession.tes.skill == "") {
      // This should not happen. The skill should be set by now.
      halt(500)
    }

    val (saPPE,saLevel) = scoringMethods.getSaPPEAndLevel(dialangSession.tes.skill,responses)

    dialangSession.saPPE = saPPE
    dialangSession.saSubmitted = true
    dialangSession.saLevel = saLevel

    saveDialangSession(dialangSession)

    dataCapture.logSAResponses(dialangSession,responses.toMap)
    dataCapture.logSAScores(dialangSession)

    if (dialangSession.tes.hideTest) {

      val url = {
          val parts = dialangSession.resultUrl.split("\\?")
          val params = new StringBuilder(if (parts.length == 2) "?" + parts(1) + "&" else "?")
          params.append("saLevel=" + saLevel)
          if (dialangSession.vsptLevel != "") params.append("&vsptLevel=" + dialangSession.vsptLevel)
          parts(0) + params.toString
        }
      if (logger.isDebugEnabled) logger.debug("Redirect URL: " + url)
      contentType = formats("json")
      "{ \"redirect\":\"" + url + "\"}"
    } else {
      contentType = formats("json")
      "{\"saLevel\":\"" + saLevel + "\",\"saDone\":\"true\"}"
    }
  }
}
