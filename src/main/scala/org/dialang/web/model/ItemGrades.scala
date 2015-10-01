package org.dialang.web.model

import java.sql.ResultSet
import scala.collection.mutable.HashMap

class ItemGrades(tl: String, skill: String, bookletId: Int, rs: ResultSet) {

  val gradeMap = {
      val map = new HashMap[Int,ItemGrade]
      while (rs.next) {
        val rawScore = rs.getInt("rsc")
        val ppe = rs.getFloat("ppe")
        val se = rs.getFloat("se")
        val grade = rs.getInt("grade")
        map += ((rawScore,new ItemGrade(ppe,se,grade)))
      }
      map.toMap
    }

  def get(rawScore: Int): Option[ItemGrade] = gradeMap.get(rawScore)
}
