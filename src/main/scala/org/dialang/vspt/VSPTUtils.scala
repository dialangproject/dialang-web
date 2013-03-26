package org.dialang.vspt

import org.dialang.db.DB

class VSPTUtils {

  val db = DB

  val levels:Map[String,Vector[(String,Int,Int)]] = db.getVSPTLevels

  /**
   *  Calculates the feedback score using Paul Meara's algorithm. Returns
   * a Tuple2 of z score and normalised (Meara'd) score
   */
  def getScore(tl:String,responses:Map[String,Boolean]):Tuple2[Double,Int] = {
    val Z = getZScore(tl,responses)

    if (Z <= 0)
      ((Z,0))
    else 
      ((Z,(Z * 1000).toInt))
  }

  private def getZScore(tl:String,responses:Map[String,Boolean]):Double = {

    val REAL = 1
    val FAKE = 0
    var yesResponses = Array(0,0)
    var noResponses = Array(0,0)

    db.getVSPTWords(tl).foreach(t => {
      val id = t._1
      val word = t._2
      val valid = t._3
      val weight = t._4

      val wordType = if(valid) REAL else FAKE

      if(responses.contains(id)) {
        if(responses(id)) {
          yesResponses(wordType) += 1
        } else {
          noResponses(wordType) += 1
        }
      }
    })

    // number of real words answered in test:
    val X = yesResponses(REAL) + noResponses(REAL)

    // number of imaginary words answered in test:
    val Y = yesResponses(FAKE) + noResponses(FAKE)

    // number of yes responses to real words:
    val H:Double = yesResponses(REAL)

    // number of yes responses to imaginary words:
    val F:Double = yesResponses(FAKE)

    if (H != 0) {
      try {
        // h: Ratio of correctly answered real words to total real words answered
        val h:Double =  H / X

        // f: Ratio of incorrectly answered fake words to total fake words answered
        val f:Double = F / Y

        (( (h - f) * (1 + h - f) ) / (h * (1 - f)) ) - 1
      }
      catch {
        case e:Exception => {
            println(e)
            0
        }
      }
    } else {
      0
    }
  }

  def getLevel(tl:String, responses:Map[String,Boolean]):Tuple3[Double,Int,String] = {
    val (zScore,mearaScore) = getScore(tl,responses)
    var level = "UNKNOWN"
    if(levels.contains(tl)) {
        val filtered = levels.get(tl).get.filter(t => mearaScore >= t._2 && mearaScore <= t._3)
      if(filtered.length == 1) {
        level = filtered(0)._1
      }
    }
    ((zScore,mearaScore,level))
  }
}
