package org.dialang.web.datacapture

import java.sql.Connection
import org.dialang.common.model.{DialangSession, ImmutableItem}

class DataCapture(dsUrl: String) {

  private val dataCapture = new DataCaptureImpl(dsUrl)

  /**
   * Sends the session details to datacapture over reliable transport
   */
  def createSessionAndPass(dialangSession: DialangSession): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.createSessionAndPass(dialangSession)
        }
      }, "createSessionAndPass").start
  }

  /**
   * Sends the pass details to datacapture over reliable transport
   */
  def createPass(dialangSession:DialangSession, suppliedConn: Connection = null): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.createPass(dialangSession)
        }
      }, "createPass").start
  }

  /**
   * Sends the vspt responses to datacapture over unreliable transport. We're not
   * too worried if we lose some of it as this is for analysis purposes only.
   * It's not 'business critical'.
   */
  def logVSPTResponses(dialangSession: DialangSession, responses: Map[String,Boolean]): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.logVSPTResponses(dialangSession, responses)
        }
      }, "logVSPTResponses").start
  }

  /**
   * Sends the vspt scores if, and only if, a userId is set on the session. This
   * is management data and only makes sense with a user id from an LTI launch.
   */
  def logVSPTScores(dialangSession: DialangSession): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.logVSPTScores(dialangSession)
        }
      }, "logVSPTScores").start
  }

  /**
   * Sends the sa responses to datacapture over unreliable transport. We're not
   * too worried if we lose some of it as this is for analysis purposes only.
   * It's not 'business critical'.
   */
  def logSAResponses(dialangSession: DialangSession, responses: Map[String,Boolean]): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.logSAResponses(dialangSession, responses)
        }
      }, "logSAResponses").start
  }

  /**
   * Sends the sa scores if, and only if, a userId is set on the session. This
   * is management data and only makes sense with a user id from an LTI launch.
   */
  def logSAScores(dialangSession: DialangSession): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.logSAScores(dialangSession)
        }
      }, "logSAPPE").start
  }

  def logTestStart(passId: String, bookletId: Int, bookletLength: Int): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.logTestStart(passId, bookletId, bookletLength)
        }
      }, "logTestStart").start
  }

  def logBasket(passId: String, basketId: Int, basketNumber: Int): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.logBasket(passId, basketId, basketNumber)
        }
      }, "logBasket").start
  }

  def logSingleIdResponse(sessionId: String, item: ImmutableItem): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.logSingleIdResponse(sessionId, item)
        }
      }, "logSingleIdResponse").start
  }

  def logMultipleTextualResponses(sessionId: String, items: List[ImmutableItem]): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.logMultipleTextualResponses(sessionId, items)
        }
      }, "logMultipleTextualResponses").start
  }

  def logMultipleIdResponses(sessionId: String, items: List[ImmutableItem]): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.logMultipleIdResponses(sessionId, items)
        }
      }, "logMultipleIdResponses").start
  }

  def logTestResult(dialangSession: DialangSession): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.logTestResult(dialangSession)
        }
      }, "logTestResult").start
  }

  def logTestFinish(passId: String): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.logTestFinish(passId)
        }
      }, "logTestFinish").start
  }

  def deleteToken(token: String): Boolean = dataCapture.deleteToken(token)

  def getScores(consumerKey: String, fromDate: String = "", toDate: String = "", userId: String = "", resourceLinkId: String = "") = dataCapture.getScores(consumerKey, fromDate, toDate, userId, resourceLinkId)

  def storeQuestionnaire(sessionId: String, data: Map[String, String]): Unit = {

    new Thread(
      new Runnable {
        def run: Unit = {
          dataCapture.storeQuestionnaire(sessionId, data)
        }
      }, "storeQuestionnaire").start
  }
}
