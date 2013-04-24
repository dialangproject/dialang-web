package org.dialang.model

import java.sql.ResultSet
import scala.collection.mutable.HashMap

class ItemGrades(tl:String,skill:String,bookletId:Int,rs:ResultSet) {

  var max = 0

  private val gradeMap = {
      val map = new HashMap[Int,ItemGrade]
      while(rs.next) {
        val rsc = rs.getInt("rsc")
        val ppe = rs.getFloat("ppe")
        val se = rs.getFloat("se")
        val grade = rs.getInt("grade")
        map += ((rsc,new ItemGrade(ppe,se,grade)))
        max = Math.max(rsc,max)
      }
      map.toMap
    }

  def get(rsc:Int): ItemGrade = gradeMap.get(rsc).get
}
