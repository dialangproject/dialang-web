package org.dialang.model

class DialangSession(map:Map[String,String]) {

  val userId = map.getOrElse("userId","")
  val sessionId = map.getOrElse("sessionId","-1").toInt
  val tl = map.getOrElse("tl","")
  val skill = map.getOrElse("skill","")
  val vsptSubmitted = map.getOrElse("vsptSubmitted","false").toBoolean
  var saSubmitted = map.getOrElse("saSubmitted","false").toBoolean
  val vsptZScore = map.getOrElse("vsptZScore","0.0").toFloat
  val vsptMearaScore = map.getOrElse("vsptMearaScore","0").toInt
  val vsptLevel = map.getOrElse("vsptLevel","V0")
  var saPPE = map.getOrElse("saPPE","0.0").toFloat
  var saResponses:Map[String,Int] = Map("blah" -> 1)
}
