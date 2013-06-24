package org.dialang.web.scoring

import org.dialang.web.db.DB
import org.dialang.common.model.ImmutableItem
import org.dialang.common.model.Item
import org.dialang.web.model.{DialangSession,ItemGrade}

import java.util.StringTokenizer

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  A utility class intended to hold all of the scoring code used by the system
 *  in one place.
 */
class ScoringMethods {

  private val logger = LoggerFactory.getLogger(getClass)

  private val db = DB

  /**
   * Need this for all cases. The assignments tells you which booklet to assign for a given lang/skill/pe.
   */
  private val assign = db.getPreestAssign

  /**
	 * and we also need the weights, this gives value to 'weight' the grading model.'s:
   */
  private val preestWeights = db.getPreestWeights

  private val saWeights = db.getSAWeights

  private val saGrades = db.getSAGrades

  /**
	 * Calculates the booklet id.
	 *
	 * If neither the VSPT nor the SA have been submitted, requests the midmost booklet assignment
   * from the the PreestAssign member. Gets the grade achieved for SA together with the
   * Z score for the VSPT, if submitted. Calls getPreestWeights to get a
   * PreestWeights object. Uses this object to get the SA and VSPT weights
   * and 'Coefficient of Mystery' for the current TLS SKILL and vspt/sa
   * submitted states. Uses the VSPT and SA weights  and coefficient to
   * calculate a theta estimate (ppe). Passes the TLS, SKILL and coefficient
   * in the PreestAssign.
	 */
  def calculateBookletId(session: DialangSession) : Int = {

		if (!session.vsptSubmitted && !session.saSubmitted) {
			// No sa or vspt, request the default assignment.
			return assign.getBookletId(session.testLanguage,session.skill)
		}

		// if either test is done, then we need to get the grade 
		// associated with that test:

		val vsptZScore = if (session.vsptSubmitted) session.vsptZScore else 0.0F

		val saPPE:Float = if(session.saSubmitted) session.saPPE else 0.0F

		// get the appropriate weight for the given context:
		val (vsptWeight,saWeight,coe) = preestWeights.get(session.testLanguage, session.skill, session.vsptSubmitted, session.saSubmitted)

		val pe =  (saPPE * saWeight.asInstanceOf[Float]) + (vsptZScore * vsptWeight) + coe

		// finaly look up the assignment for the resulting values:
		assign.getBookletId(session.testLanguage,session.skill,pe)
	}

  /**
   * Returns the sum of the weights of the questions answered 'true'
   */
  private def getSaRawScore(skill: String,responses: Map[String,Boolean]):Int = {

    responses.keys.foldLeft(0)( (rsc,id) => {
      if(responses.get(id).get) {
        // They responded true to this statement, add its weight.
        saWeights.get(skill) match {
            case Some(wordMap) => {
              rsc + wordMap.get(id).get
            }
            case None => {
              logger.error("Failed to get word map for skill: " + skill)
              rsc
            }
          }
      } else {
        rsc
      }
    })
  }

  def getSaPPEAndLevel(skill: String,responses: Map[String,Boolean]):Tuple2[Float,String] = {
    val rsc = getSaRawScore(skill,responses)
    val levelMap = db.getLevels
    ((saGrades.getPPE(skill,rsc),levelMap.get(saGrades.getGrade(skill,rsc)).get))
  }

  /**
   * Used for mcq and gap drop
   */
  def getScoredIdResponseItem(itemId:Int,responseId:Int): Option[Item] = {

    db.getItem(itemId) match {
        case Some(item) => { 
          item.responseId = responseId
          db.getAnswer(responseId) match {
              case Some(answer) => {
                if(answer.correct) {
                  item.correct = true
                  item.score = item.weight
                } else {
                  item.correct = false
                }
                Some(item)
              }
              case None => {
                // The answerId couldn't be found in the system.
                None
              }
            }
        }
        case None => {
          // The itemId couldn't be found in the system.
          None
        }
      }
  }

  /**
   * Used for short answer and gap text
   */
  def getScoredTextResponseItem(itemId:Int,answerText:String): Option[Item] = {

    db.getItem(itemId) match {
        case Some(item) => {
          item.responseText = answerText
          var score = 0
          db.getAnswers(itemId) match {
              case Some(answers) => {
                answers.foreach(correctAnswer => {
                  if(removeWhiteSpaceAndPunctuation(correctAnswer.text).equalsIgnoreCase(removeWhiteSpaceAndPunctuation(answerText))) {
                    item.score = item.weight;
                    item.correct = true
                  }
                })
                Some(item)
              }
              case None => {
                logger.error("No answers for item id " + itemId + ". Returning None ...")
                None
              }
            }
        }
        case None => {
          logger.error("No item for item id " + itemId + ". Returning None ...")
          None
        }
      }
  }

  def getItemGrade(session:DialangSession,results:List[ImmutableItem]):Tuple2[Int,String] = {

    var rawScore = 0
    var weight = 0

    // results contains the set of results for the entire test - ie the
    // results for each item type.

    results.foreach(item => {

      // total weights:
      weight += item.weight

      // total score:
      rawScore += item.score
    })

    val itemGrades = db.getItemGrades(session.testLanguage, session.skill, session.bookletId)

    // normalize:
    rawScore = ((rawScore.toFloat) * (itemGrades.max / weight.toFloat)).toInt

    val itemGrade = itemGrades.get(rawScore) match {
        case Some(ig) => ig
        case None => {
          logger.error("No item grade for raw score " + rawScore + ".")
          new ItemGrade(0,0,0)
        }
      }

    ((itemGrade.grade,db.getLevels.get(itemGrade.grade).get))
  }

  /**
   *  Trim leading and trailing whitespace and then replace tab, newline,
   *  carriage return and form-feed characters with a whitespace.
   */
  private def removeWhiteSpaceAndPunctuation(in:String) = {

    val punctuationList = db.getPunctuationCharacters

    if(punctuationList.size > 0) {

      // Trim the white space and tokenize it around default delimiters
      val st = new StringTokenizer (in.trim)

      val firstPass = new StringBuffer(32)

      // Rebuild it with single spaces in between tokens.
      while (st.hasMoreTokens) {
        firstPass.append(st.nextToken + " ")
      }

      // Rebuild the string without punctuation defined in the punctuation list.
      val secondPass = firstPass.toString.filter(c => {
          val hexString = Integer.toHexString(c.charValue)
          punctuationList.contains(hexString) == false
        })

      secondPass.toString
    } else {
      // The punctuation list couldn't be found in the system.
      // TODO: Logging
      in
    }
  }
}
