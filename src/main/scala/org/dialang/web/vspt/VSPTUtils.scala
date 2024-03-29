package org.dialang.web.vspt

import org.dialang.db.{DB, DBFactory}

import grizzled.slf4j.Logging

class VSPTUtils(db:DB = DBFactory.get()) extends Logging {

  /**
   * Calculates the feedback score using Paul Meara's algorithm. Returns
   * a Tuple2 of z score and normalised (Meara'd) score
   */
  def getScore(tl:String,responses:Map[String,Boolean]):Tuple2[Double,Int] = {

    val Z = getZScore(tl,responses)

    if (Z <= 0) {
      ((Z,0))
    } else {
      ((Z,(Z * 1000).toInt))
    }
  }

  private def getZScore(tl: String, responses: Map[String, Boolean]): Double = {

    val REAL = 1
    val FAKE = 0
    val yesResponses = Array(0,0)
    val noResponses = Array(0,0)

    db.getVSPTWords(tl) match {

      case Some(list) => {
        list.foreach(word => {

          val wordType = if (word.valid) REAL else FAKE

          if (responses.contains(word.id)) {
            if (responses(word.id)) {
              yesResponses(wordType) += 1
            } else {
              noResponses(wordType) += 1
            }
          } else {
            logger.error("The responses did not contain the word with id '" + word.id + "'. This is incorrect.")
          }
        })
      }
      case None => {
        logger.error("No VSPT word list defined for test language '" + tl + "'.")
      }
    }

    val realWordsAnswered = yesResponses(REAL) + noResponses(REAL)
    logger.debug("realWordsAnswered: " + realWordsAnswered)

    val fakeWordsAnswered = yesResponses(FAKE) + noResponses(FAKE)
    logger.debug("fakeWordsAnswered: " + fakeWordsAnswered)

    // Hits. The number of yes responses to real words.
    val hits = yesResponses(REAL)
    logger.debug("hits: " + hits);

    // False alarms. The number of yes responses to fake words.
    val falseAlarms = yesResponses(FAKE)
    logger.debug("falseAlarms: " + falseAlarms)

    if(hits == 0) {
      // No hits whatsoever results in a zero score
      0
    } else {
      VSPTScoringAlgorithms.getVersion10ZScore(hits, realWordsAnswered, falseAlarms, fakeWordsAnswered)
    }
  }

  def getBand(tl: String, responses: Map[String, Boolean]): Tuple3[Double, Int, String] = {

    val (zScore,mearaScore) = getScore(tl,responses)

    logger.debug("zScore: " + zScore + ". mearaScore: " + mearaScore)

    val level = db.vsptBands.get(tl) match {
        case Some(band: Vector[(String, Int, Int)]) => {
          val filtered = band.filter(t => mearaScore >= t._2 && mearaScore <= t._3)
          if(filtered.length == 1) {
            filtered(0)._1
          } else {
            logger.error("No level for test language '" + tl + "' and meara score: " + mearaScore + ". Returning UNKNOWN ...")
            "UNKNOWN"
          }
        }
        case _ => {
          logger.error("No band found for test language '" + tl + "'. Returning UNKNOWN ...")
          "UNKNOWN"
        }
      }

    logger.debug("Level: " + level)

    ((zScore,mearaScore,level))
  }
}
