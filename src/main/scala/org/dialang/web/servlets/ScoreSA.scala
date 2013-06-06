package org.dialang.web.servlets

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import org.dialang.web.model.DialangSession
import org.dialang.web.scoring.ScoringMethods

import org.slf4j.LoggerFactory;

class ScoreSA extends DialangServlet {

  private val logger = LoggerFactory.getLogger(getClass)

  private val scoringMethods = new ScoringMethods

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

    if(dialangSession.testLanguage == "" || dialangSession.skill == "") {
      // This should not happen. The skill should be set by now.
      halt(500)
    }

    val (saPPE,saLevel) = scoringMethods.getSaPPEAndLevel(dialangSession.skill,responses)

    dialangSession.saPPE = saPPE
    dialangSession.saSubmitted = true
    dialangSession.saLevel = saLevel

    saveDialangSession(dialangSession)

    dataCapture.logSAResponses(dialangSession,responses.toMap)
    dataCapture.logSAPPE(dialangSession)

   contentType = "application/json"
   "{\"saLevel\":\"" + saLevel + "\",\"saDone\":\"true\"}"
  }
}
