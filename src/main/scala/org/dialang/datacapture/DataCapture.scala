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
}
