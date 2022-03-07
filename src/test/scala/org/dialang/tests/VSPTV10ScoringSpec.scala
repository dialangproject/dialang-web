package org.dialang.tests

import org.dialang.db.DBFactory
import org.dialang.common.model.VSPTWord
import org.dialang.web.vspt.{VSPTUtils, VSPTScoringAlgorithms}

import java.sql.SQLException

import scala.collection.mutable.HashMap

import org.scalatest._
import flatspec.AnyFlatSpec
import matchers.should.Matchers._

class VSPTV10ScoringSpec extends AnyFlatSpec {

  "getVersion10ZScore(H=50,X=0,F=25,Y=0)" should " result in an IllegalArgumentException" in {

    // 10 of the real words hit and 11 false alarms.
    try {
      val zScore = VSPTScoringAlgorithms.getVersion10ZScore(50,0,25,0)
      fail()
    } catch {
      case ile:IllegalArgumentException => {}
      case e : Exception => fail()
    }
  }

  "getVersion10ZScore(H=50,X=50,F=0,Y=25)" should " result in a score of 1.0" in {

    // All the real words hit and no false alarms.
    val zScore = VSPTScoringAlgorithms.getVersion10ZScore(50,50,0,25)
    zScore should equal (1.0D)
  }

  "getVersion10ZScore(H=50,X=50,F=25,Y=25)" should " result in a score of 1.0" in {

    // All of the real words hit and 25 false alarms. This would happen if the taker just
    // clicked green for all the words
    val zScore = VSPTScoringAlgorithms.getVersion10ZScore(50,50,25,25)
    zScore should equal (-1.0D)
  }

  "getVersion10ZScore(H=25,X=50,F=25,Y=25)" should " result in a score of -1.0" in {

    // 25 of the real words hit and 25 false alarms.
    val zScore = VSPTScoringAlgorithms.getVersion10ZScore(25,50,25,25)
    zScore should equal (-1.0D)
  }

  "getVersion10ZScore(H=10,X=50,F=11,Y=25)" should " result in a score of -0.2893401015 (to 10 d.p.)" in {

    // 10 of the real words hit and 11 false alarms.
    val zScore = VSPTScoringAlgorithms.getVersion10ZScore(10,50,11,25)
    "%.10f".format(zScore).toDouble should equal (-0.2893401015D)
  }

  "getVersion10ZScore(H=31,X=50,F=7,Y=25)" should " result in a score of 0.342556391 (to 9 d.p.)" in {

    // 10 of the real words hit and 11 false alarms.
    val zScore = VSPTScoringAlgorithms.getVersion10ZScore(31,50,7,25)
    "%.9f".format(zScore).toDouble should equal (0.342556391D)
  }
}
