package org.dialang.model

import scala.collection.mutable.HashMap

class SAGrades {

  val grades = new HashMap[String,HashMap[Int,Tuple3[Float,Float,Int]]]

  def +=(skill:String,rawScore:Int,ppe:Float,se:Float,grade:Int):SAGrades = {
    if(!grades.contains(skill)) {
        grades += (skill -> new HashMap[Int,Tuple3[Float,Float,Int]])
    }
    val scoreMap = grades.get(skill).get
    scoreMap += (rawScore -> (ppe,se,grade))
    this
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
