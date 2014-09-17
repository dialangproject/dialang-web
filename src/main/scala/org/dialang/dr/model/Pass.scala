package org.dialang.dr.model

class Pass(val id:String,val al:String,val tl:String,val skill:String) {
  
  var vsptResponseData:Option[VSPTResponseData] = None
  var saResponseData:Option[SAResponseData] = None
  var itemResponseData:Option[ItemResponseData] = None
}
