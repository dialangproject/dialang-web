package org.dialang.web.model

import scala.collection.mutable.HashMap

import java.sql.ResultSet

class SAGrades(rs:ResultSet) {

  private val grades:Map[String,Map[Int,Tuple3[Float,Float,Int]]] = {
      val tmp = new HashMap[String,HashMap[Int,Tuple3[Float,Float,Int]]]
      while(rs.next) {
        val skill = rs.getString("skill")
        val rawScore = rs.getInt("rsc")
        val ppe = rs.getFloat("ppe")
        val se = rs.getFloat("se")
        val grade = rs.getInt("grade")
        if(!tmp.contains(skill)) {
          tmp += (skill -> new HashMap[Int,Tuple3[Float,Float,Int]])
        }
        val scoreMap = tmp.get(skill).get
        scoreMap += (rawScore -> (ppe,se,grade))
      }
      tmp.map(t => ((t._1,t._2.toMap))).toMap
    }

  def getPPE(skill:String,rawScore:Int):Float = {
    grades.get(skill).get(rawScore)._1
  }

  /**
   * Gets the numeric grade for the supplied skill and raw score combo.
   * Grades should be from 1 to 6 inclusive as of 23/04/2013, and this
   * range may well increase as new levels are added
   */
  def getGrade(skill:String,rawScore:Int):Int = {
    grades.get(skill).get(rawScore)._3
  }
}
