package org.dialang.model

class DialangSession(map:Map[String,String]) {

  val tl = map.getOrElse("tl","")
  val skill = map.getOrElse("skill","")
  val vsptSubmitted = map.getOrElse("vsptSubmitted","false").toBoolean
  var saSubmitted = map.getOrElse("saSubmitted","false").toBoolean
  val vsptZScore = map.getOrElse("vsptZScore","0.0").toFloat
  var saPPE = map.getOrElse("saPPE","0.0").toFloat
  var saResponses:Map[String,Int] = Map("blah" -> 1)
}
