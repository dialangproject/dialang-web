package org.dialang.vspt

import java.sql.{DriverManager, Connection}

import org.dialang.db.DB

class VSPTUtils {

  Class.forName("org.postgresql.Driver")
  val conn = DriverManager.getConnection("jdbc:postgresql:DIALANG","dialangadmin","dialangadmin")

  val db = new DB

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
    // TODO: Move this into DB layer
    val st = conn.createStatement
    val rs = st.executeQuery("SELECT words.word_id,words.word,words.valid,words.weight FROM vsp_test_word,words WHERE locale = '" + tl + "' AND vsp_test_word.word_id = words.word_id")
    while(rs.next) {
      val id = rs.getString("WORD_ID")
      val word = rs.getString("WORD")
      val valid = rs.getBoolean("VALID")
      val weight = rs.getInt("WEIGHT")
      val wordType = if(valid) REAL else FAKE

      if(responses.contains(id)) {
        if(responses(id))
          yesResponses(wordType) += 1
        else
          noResponses(wordType) += 1
      }
    }
    rs.close
    st.close

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
