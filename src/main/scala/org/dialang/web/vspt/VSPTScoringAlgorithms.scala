package org.dialang.web.vspt

object VSPTScoringAlgorithms {

  def getVersion6ZScore(hits: Int, realWordsAnswered: Int,falseAlarms: Int, fakeWordsAnswered: Int):Double = {

    require(hits >= 0 && realWordsAnswered > 0 && falseAlarms >= 0 && fakeWordsAnswered > 0)

    // The observed hit rate. Hits divided by the  total number of real words answered.
    val h =  hits.toDouble / realWordsAnswered.toDouble

    // The false alarm rate. False alarms divided by the total number of fake words answered.
    val f = falseAlarms.toDouble / fakeWordsAnswered.toDouble

    try {
      val lhs = ( ( (h - f) * (1 + h - f) ) / (h * (1 - f)) )
      if(lhs.isNaN || lhs.isInfinity) {
        // This can arise if h is zero or f is 1, thus making the denominator zero. We're
        // assuming that divide by zero gives zero, so the result is -1.
        -1
      } else {
        lhs - 1
      }
    } catch {
      case e:Exception => {
        0
      }
    }
  }

  def getVersion10ZScore(hits: Int, realWordsAnswered: Int,falseAlarms: Int, fakeWordsAnswered: Int):Double = {

    require(hits >= 0 && realWordsAnswered > 0 && falseAlarms >= 0 && fakeWordsAnswered > 0
              , "One of these conditions failed. hits >= 0, realWordsAnswered > 0, falseAlarms >= 0, fakeWordsAnswered > 0")

    // The observed hit rate. Hits divided by the  total number of real words answered.
    val h =  hits.toDouble / realWordsAnswered.toDouble

    // The false alarm rate. False alarms divided by the total number of fake words answered.
    val f = falseAlarms.toDouble / fakeWordsAnswered.toDouble

    if (h == 1 && f == 1) {
      // This means the test taker has just clicked green for all the words
      -1
    } else {
      try {
        val rhs = (( 4 * h * (1 - f) ) - (2 * (h - f) * (1 + h - f))) / ((4 * h * (1 - f)) - ((h - f) * (1 + h - f)))
          1 - rhs
      } catch {
        case e:Exception => {
          0
        }
      }
    }
  }

  def main(args:Array[String]) {

    require(args.size == 4)

    println("v6ZScore: " + getVersion6ZScore(args(0).toInt,args(1).toInt,args(2).toInt,args(3).toInt))
    println("v10ZScore: " + getVersion10ZScore(args(0).toInt,args(1).toInt,args(2).toInt,args(3).toInt))
  }
}
