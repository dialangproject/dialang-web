package org.dialang.web.servlets

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import org.dialang.db.DBFactory
import org.dialang.web.vspt.VSPTUtils

import org.json4s.{DefaultFormats, Formats}

import org.scalatra.json._

import org.slf4j.LoggerFactory;

class ScoreVSPT extends DialangServlet with JacksonJsonSupport {

  private val logger = LoggerFactory.getLogger(getClass)

  private val vsptUtils = new VSPTUtils

  protected implicit val jsonFormats: Formats = DefaultFormats

  post("/") {

    val responses = new HashMap[String,Boolean]
    params.foreach(t => {
      val name = t._1.asInstanceOf[String]
      if (name.startsWith("word:")) {
        val id = name.split(":")(1)
        val answer = t._2 match {
            case "valid" => true
            case "invalid" => false
          }

        responses += (id -> answer)
      }
    })

    val dialangSession = getDialangSession

    // This is a Tuple3 of zscore, meara score and level.
    val (zScore,mearaScore,level) = vsptUtils.getBand(dialangSession.tes.tl,responses.toMap)

    dialangSession.vsptZScore = zScore.toFloat
    dialangSession.vsptMearaScore = mearaScore
    dialangSession.vsptLevel = level
    dialangSession.vsptSubmitted = true

    saveDialangSession(dialangSession)

    dataCapture.logVSPTResponses(dialangSession, responses.toMap)
    dataCapture.logVSPTScores(dialangSession)

    if (dialangSession.tes.hideSA && dialangSession.tes.hideTest && dialangSession.resultUrl != "") {
        val url = {
            val parts = dialangSession.resultUrl.split("\\?")
            val params = new StringBuilder(if (parts.length == 2) "?" + parts(1) + "&" else "?")
            params.append("vsptLevel=" + level)
            parts(0) + params.toString
          }
        if (logger.isDebugEnabled) logger.debug("Redirect URL: " + url)
        contentType = formats("json")
        "{ \"redirect\":\"" + url + "\"}"
      } else {
        contentType = formats("json")
        "{ \"vsptMearaScore\":\"" + mearaScore.toString + "\",\"vsptLevel\":\"" + level + "\",\"vsptDone\": \"true\" }"
      }
  }
}
