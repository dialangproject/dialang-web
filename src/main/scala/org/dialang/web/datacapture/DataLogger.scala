package org.dialang.web.datacapture

import org.dialang.web.model.DialangSession

import java.io.{OutputStreamWriter,BufferedWriter,IOException}
import java.net.{InetAddress,Socket}

object DataLogger {

  /**
   * Sends the session details to datacapture over reliable transport
   */
  def createSession(dialangSession:DialangSession, ipAddress:String) {

    new Thread(
      new Runnable {
        def run {
          val data = "SESS:|%s|%s|%s|%s|%s|%s|%s".format(
                                                  dialangSession.sessionId,
                                                  dialangSession.userId,
                                                  dialangSession.consumerKey,
                                                  dialangSession.adminLanguage,
                                                  dialangSession.testLanguage,
                                                  dialangSession.skill,
                                                  ipAddress)

          try {
            val sock = new Socket(InetAddress.getByName("localhost"),5555)
            val writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream))
            writer.write(data)
            writer.flush
            sock.close()
          } catch {
            case ioe:IOException => // TODO: Cache this for a later attempt
            case se:SecurityException => // TODO: Cache this for a later attempt
          }
        }
      }).start
  }

  /**
   * Sends the vspt responses to datacapture over unreliable transport. We're not
   * too worried if we lose some of it as this is for analysis purposes only.
   * It's not 'business critical'.
   */
  /*
  def logVSPTResponses(dialangSession: DialangSession, responses: Map[String,Boolean]) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logVSPTResponses(dialangSession, responses)
        }
      }).start
  }
  */

  /**
   * Sends the vspt scores if, and only if, a userId is set on the session. This
   * is management data and only makes sense with a user id from an LTI launch.
   */
  /*
  def logVSPTScores(dialangSession: DialangSession) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logVSPTScores(dialangSession)
        }
      }).start
  }
  */

  /**
   * Sends the sa responses to datacapture over unreliable transport. We're not
   * too worried if we lose some of it as this is for analysis purposes only.
   * It's not 'business critical'.
   */
  /*
  def logSAResponses(dialangSession: DialangSession, responses: Map[String,Boolean]) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logSAResponses(dialangSession, responses)
        }
      }).start
  }
  */

  /**
   * Sends the sa scores if, and only if, a userId is set on the session. This
   * is management data and only makes sense with a user id from an LTI launch.
   */
  /*
  def logSAPPE(dialangSession: DialangSession) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logSAPPE(dialangSession)
        }
      }).start
  }

  def logSingleIdResponse(sessionId: String, basketId: Int, itemId: Int, answerId: Int) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logSingleIdResponse(sessionId, basketId, itemId, answerId)
        }
      }).start
  }

  def logMultipleTextualResponses(sessionId: String, basketId: Int, responses: Map[Int,String]) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logMultipleTextualResponses(sessionId, basketId, responses)
        }
      }).start
  }

  def logMultipleIdResponses(sessionId: String, basketId: Int, responses: Map[Int,Int]) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logMultipleIdResponses(sessionId, basketId, responses)
        }
      }).start
  }

  def logTestResult(dialangSession:DialangSession) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logTestResult(dialangSession)
        }
      }).start
  }
  */
}
