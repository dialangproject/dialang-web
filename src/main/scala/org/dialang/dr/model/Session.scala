package org.dialang.dr.model

class Session(val id:String,val ipAddress:String,val startedMillisSinceEpoch:Long) {
  
  var passes:List[Pass] = List[Pass]()
}
