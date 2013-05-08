package org.dialang.web.model

import java.sql.ResultSet
import scala.collection.mutable.HashMap

class ItemGrades(tl:String,skill:String,bookletId:Int,rs:ResultSet) {

  val (gradeMap,max) = {
      val map = new HashMap[Int,ItemGrade]
      var tmpMax = 0
      while(rs.next) {
        val rawScore = rs.getInt("rsc")
        val ppe = rs.getFloat("ppe")
        val se = rs.getFloat("se")
        val grade = rs.getInt("grade")
        map += ((rawScore,new ItemGrade(ppe,se,grade)))
        tmpMax = Math.max(rawScore,tmpMax)
      }
      (map.toMap,tmpMax)
    }

  def get(rawScore:Int):Option[ItemGrade] = gradeMap.get(rawScore)
}
