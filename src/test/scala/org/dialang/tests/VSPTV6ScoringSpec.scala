package org.dialang.tests

import org.dialang.db.DBFactory
import org.dialang.common.model.VSPTWord
import org.dialang.web.vspt.{VSPTUtils,VSPTScoringAlgorithms}

import java.sql.SQLException

import scala.collection.mutable.HashMap

import org.scalatest._

class VSPTV6ScoringSpec extends FlatSpec with Matchers {

  "getVersion6ZScore(H=50,X=0,F=25,Y=0)" should " result in an IllegalArgumentException" in {

    // 10 of the real words hit and 11 false alarms.
    try {
      val zScore = VSPTScoringAlgorithms.getVersion6ZScore(50,0,25,0)
      fail
    } catch {
      case ile:IllegalArgumentException => {}
      case e : Exception => fail
    }
  }

  "getVersion6ZScore(H=50,X=50,F=0,Y=25)" should " result in a score of 1.0" in {

    // All the real words hit and no false alarms.
    val zScore = VSPTScoringAlgorithms.getVersion6ZScore(50,50,0,25)
    zScore should equal (1.0D)
  }

  "getVersion6ZScore(H=50,X=50,F=25,Y=25)" should " result in a score of -1.0" in {

    // All of the real words hit and 25 false alarms.
    val zScore = VSPTScoringAlgorithms.getVersion6ZScore(50,50,25,25)
    zScore should equal (-1.0D)
  }

  "getVersion6ZScore(H=25,X=50,F=25,Y=25)" should " result in a score of -1.0" in {

    // 25 of the real words hit and 25 false alarms.
    val zScore = VSPTScoringAlgorithms.getVersion6ZScore(25,50,25,25)
    zScore should equal (-1.0D)
  }

  "getVersion6ZScore(H=10,X=50,F=11,Y=25)" should " result in a score of -2.628571429 (to 9 d.p.)" in {

    // 10 of the real words hit and 11 false alarms.
    val zScore = VSPTScoringAlgorithms.getVersion6ZScore(10,50,11,25)
    "%.9f".format(zScore).toDouble should equal (-2.628571429D)
  }

  "getVersion6ZScore(H=31,X=50,F=7,Y=25)" should " result in a score of 0.020609319 (to 9 d.p.)" in {

    // 10 of the real words hit and 11 false alarms.
    val zScore = VSPTScoringAlgorithms.getVersion6ZScore(31,50,7,25)
    "%.9f".format(zScore).toDouble should equal (0.020609319D)
  }
}
