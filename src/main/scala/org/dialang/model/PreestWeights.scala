package org.dialang.model

import scala.collection.mutable.HashMap

/**
 * The Preestimation weightings table is stored as an array of four hashmaps, indexed on vspt and sa boolean
 * values, through the map function. Note that the index [0][0] is actually illegal, but as there is a map at
 * that location, the get is safe and will by default return null - nothing found.
 */
class PreestWeights {

  val weights = new HashMap[String,Tuple3[Float,Float,Float]]

  def createKey(tl:String, skill:String, vDone:Boolean, sDone:Boolean) = {
    tl + "#" + skill + "#" + vDone + "#" + sDone
  }

	def add(tl:String, skill:String, vsptDone:Boolean, saDone:Boolean, vspt:Float, sa:Float, coe:Float) {
    weights += (createKey(tl,skill,vsptDone,saDone) -> (vspt,sa,coe))
	}

	def get(tl:String, skill:String, vsptDone:Boolean, saDone:Boolean):Tuple3[Float,Float,Float] = {
    weights(createKey(tl,skill,vsptDone,saDone))
	}

	override def toString:String = {
		val sb = new StringBuffer

    weights.foreach(t => {
      val (key,value) = t
      sb.append("{" + key + ":")
      sb.append("[" + value._1)
      sb.append("," + value._2)
      sb.append("," + value._3 + "]}")
      sb.append(System.getProperty("line.separator"))
    })

		sb.toString
	}
}
