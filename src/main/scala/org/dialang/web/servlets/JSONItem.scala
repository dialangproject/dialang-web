package org.dialang.web.servlets

case class JSONItem(id:Int,basketId:Int,responseId:Int,responseText:String,itemType:String,skill:String,subskill:String,positionInTest:Int,positionInBasket:Int,weight:Int,score:Int,correct:Boolean,answers:List[JSONAnswer])
