package org.dialang.web.model

import java.sql.ResultSet

/**
 * The Preestimation weightings table is stored as an array of four hashmaps, indexed on vspt and sa boolean
 * values, through the map function. Note that the index [0][0] is actually illegal, but as there is a map at
 * that location, the get is safe and will by default return null - nothing found.
 */
class PreestWeights(rs: ResultSet) {

  private def createKey(tl: String, skill: String, vDone: Boolean, sDone: Boolean) = {
    tl + "#" + skill + "#" + vDone + "#" + sDone
  }

  private val weights: Map[String,Map[String, Float]] = {

      val builder = Map.newBuilder[String, Map[String, Float]]
      while (rs.next) {
        val tl = rs.getString("tl")
        val skill = rs.getString("skill")
        val vsptDone = rs.getBoolean("vspttaken")
        val saDone = rs.getBoolean("sataken")
        val vspt = rs.getFloat("vspt")
        val sa = rs.getFloat("sa")
        val coe = rs.getFloat("coe")
        builder += (createKey(tl, skill, vsptDone, saDone) -> Map("vspt" -> vspt, "sa" -> sa, "coe" -> coe))
      }
      builder.result
    }

	def get(tl: String, skill: String, vsptDone: Boolean, saDone: Boolean): Option[Map[String, Float]] = {

    weights.get(createKey(tl, skill, vsptDone, saDone))
	}

	override def toString: String = {

		val sb = new StringBuffer

    weights.foreach(t => {
      val (key, value) = t
      sb.append("{" + key + ":")
      sb.append("[" + value.get("vspt").get)
      sb.append("," + value.get("sa").get)
      sb.append("," + value.get("coe").get)
      sb.append(System.getProperty("line.separator"))
    })

		sb.toString
	}
}
