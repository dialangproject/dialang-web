package org.dialang.model

class DialangSession(map:Map[String,String]) {

  val userId = map.getOrElse("userId","")
  val consumerKey = map.getOrElse("consumerKey","")
  val al = map.getOrElse("al","")
  val sessionId = map.getOrElse("sessionId","")
  val tl = map.getOrElse("tl","")
  val skill = map.getOrElse("skill","")
  val vsptSubmitted = map.getOrElse("vsptSubmitted","false").toBoolean
  val saSubmitted = map.getOrElse("saSubmitted","false").toBoolean
  val vsptZScore = map.getOrElse("vsptZScore","0.0").toFloat
  val vsptMearaScore = map.getOrElse("vsptMearaScore","0").toInt
  val vsptLevel = map.getOrElse("vsptLevel","V0")
  val saPPE = map.getOrElse("saPPE","0.0").toFloat
  val saLevel = map.getOrElse("saLevel","")
  val itemLevel = map.getOrElse("itemLevel","")
  val saResponses:Map[String,Int] = Map("blah" -> 1)
  val bookletId = map.getOrElse("bookletId","0").toInt
  val currentBasketNumber = map.getOrElse("currentBasketNumber","0").toInt
}
