package org.dialang.tests

import org.dialang.web.model.DialangSession

import java.util.{Date,UUID}

import java.sql.SQLException

import org.dialang.web.datacapture.DataCaptureImpl

import org.scalatest._

class DataCaptureSpec extends FlatSpec with Matchers {

  "createSessionAndPass" should "take a DialangSession and create new session and pass records" in {

    val dialangSession = new DialangSession
    dialangSession.sessionId = UUID.randomUUID.toString
    dialangSession.userId = UUID.randomUUID.toString
    dialangSession.consumerKey = UUID.randomUUID.toString
    dialangSession.ipAddress = "127.0.0.1"
    dialangSession.started = (new Date).getTime

    dialangSession.passId = UUID.randomUUID.toString
    dialangSession.adminLanguage = "eng_gb"
    dialangSession.testLanguage = "spa_es"
    dialangSession.skill = "reading"

    val dataCapture = new DataCaptureImpl("jdbc.dialangdatacapture")

    dataCapture.createSessionAndPass(dialangSession)

    try {

      val storedDialangSession = dataCapture.getSession(dialangSession.sessionId) match {
          case Some(ds:DialangSession) => ds
          case None => new DialangSession
        }

      val storedDialangPass = dataCapture.getPass(dialangSession.passId) match {
          case Some(ds:DialangSession) => ds
          case None => new DialangSession
        }

      storedDialangSession.sessionId should equal (dialangSession.sessionId)
      storedDialangSession.userId should equal (dialangSession.userId)
      storedDialangSession.consumerKey should equal (dialangSession.consumerKey)
      storedDialangSession.ipAddress should equal (dialangSession.ipAddress)
      storedDialangSession.started should equal (dialangSession.started)

      storedDialangPass.passId should equal (dialangSession.passId)
      storedDialangPass.adminLanguage should equal (dialangSession.adminLanguage)
      storedDialangPass.testLanguage should equal (dialangSession.testLanguage)
      storedDialangPass.skill should equal (dialangSession.skill)
    } catch {
      case s:SQLException => {
        println(s.getMessage)
        fail
      }
    }
  }
}
