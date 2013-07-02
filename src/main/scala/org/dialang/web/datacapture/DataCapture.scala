package org.dialang.web.datacapture

import org.dialang.web.model.DialangSession

class DataCapture {

  private val dataCapture = new DataCaptureImpl

  /**
   * Sends the session details to datacapture over reliable transport
   */
  def createSession(dialangSession:DialangSession, ipAddress:String) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.createSession(dialangSession,ipAddress)
        }
      }).start
  }

  /**
   * Sends the vspt responses to datacapture over unreliable transport. We're not
   * too worried if we lose some of it as this is for analysis purposes only.
   * It's not 'business critical'.
   */
  def logVSPTResponses(dialangSession: DialangSession, responses: Map[String,Boolean]) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logVSPTResponses(dialangSession, responses)
        }
      }).start
  }

  /**
   * Sends the vspt scores if, and only if, a userId is set on the session. This
   * is management data and only makes sense with a user id from an LTI launch.
   */
  def logVSPTScores(dialangSession: DialangSession) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logVSPTScores(dialangSession)
        }
      }).start
  }

  /**
   * Sends the sa responses to datacapture over unreliable transport. We're not
   * too worried if we lose some of it as this is for analysis purposes only.
   * It's not 'business critical'.
   */
  def logSAResponses(dialangSession: DialangSession, responses: Map[String,Boolean]) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logSAResponses(dialangSession, responses)
        }
      }).start
  }

  /**
   * Sends the sa scores if, and only if, a userId is set on the session. This
   * is management data and only makes sense with a user id from an LTI launch.
   */
  def logSAPPE(dialangSession: DialangSession) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logSAPPE(dialangSession)
        }
      }).start
  }

  def logTestStart(passId:String) {
    new Thread(
      new Runnable {
        def run {
          dataCapture.logTestStart(passId)
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

  def logTestFinish(passId:String) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logTestFinish(passId)
        }
      }).start
  }
}
