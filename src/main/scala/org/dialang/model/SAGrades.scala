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
}
