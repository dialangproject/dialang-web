package org.dialang.datacapture

class DataCapture {

  val dataCapture = new DataCaptureImpl

  def createSession(sessionId:String, userId:String, consumerKey:String, al:String, tl:String, skill:String, ipAddress:String) {

    new Thread(
      new Runnable {
        def run {
          println("Running session thread")
          dataCapture.createSession(sessionId, userId, consumerKey, al, tl, skill, ipAddress)
        }
      }).start
  }

  def logVSPTResponsesAndScores(sessionId: String, responses: Map[String,Boolean],zScore: Double,mearaScore: Int,level: String) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logVSPTResponsesAndScores(sessionId, responses,zScore,mearaScore,level)
        }
      }).start
  }

  def logSAResponsesAndPPE(sessionId: String, responses: Map[String,Boolean],ppe: Float) {

    new Thread(
      new Runnable {
        def run {
          dataCapture.logSAResponsesAndPPE(sessionId, responses,ppe)
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
}
