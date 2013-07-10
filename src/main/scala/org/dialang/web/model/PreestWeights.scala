package org.dialang.web.model

import scala.collection.mutable.HashMap

import java.sql.ResultSet

/**
 * The Preestimation weightings table is stored as an array of four hashmaps, indexed on vspt and sa boolean
 * values, through the map function. Note that the index [0][0] is actually illegal, but as there is a map at
 * that location, the get is safe and will by default return null - nothing found.
 */
class PreestWeights(rs:ResultSet) {

  private def createKey(tl:String, skill:String, vDone:Boolean, sDone:Boolean) = {
    tl + "#" + skill + "#" + vDone + "#" + sDone
  }

  private val weights:Map[String,Tuple3[Float,Float,Float]] = {
      val tmp = new HashMap[String,Tuple3[Float,Float,Float]]
      while(rs.next) {
        val tl = rs.getString("tl")
        val skill = rs.getString("skill")
        val vsptDone = rs.getBoolean("vspttaken")
        val saDone = rs.getBoolean("sataken")
        val vspt = rs.getFloat("vspt")
        val sa = rs.getFloat("sa")
        val coe = rs.getFloat("coe")
        tmp += (createKey(tl,skill,vsptDone,saDone) -> (vspt,sa,coe))
      }
      tmp.toMap
    }

	def get(tl:String, skill:String, vsptDone:Boolean, saDone:Boolean):Option[Tuple3[Float,Float,Float]] = {
    weights.get(createKey(tl,skill,vsptDone,saDone))
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
