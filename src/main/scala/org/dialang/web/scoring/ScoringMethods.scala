package org.dialang.web.scoring

import org.dialang.web.db.DBFactory
import org.dialang.common.model.{ImmutableItem, Item, ScoredItem}
import org.dialang.web.model.{DialangSession, ItemGrade}

import java.util.StringTokenizer

import org.slf4j.Logger;
import org.slf4j.LoggerFactory

/**
 *  A utility class intended to hold all of the scoring code used by the system
 *  in one place.
 */
class ScoringMethods {

  private val logger = LoggerFactory.getLogger(getClass)

  private val db = DBFactory.get()

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
  def calculateBookletId(session: DialangSession): Int = {

    if (session.tes.testDifficulty != "") {
      session.tes.testDifficulty match {
        case "easy" => {
          db.preestAssign.getEasyBookletId(session.tes.tl, session.tes.skill)
        }
        case "medium" => {
          db.preestAssign.getMediumBookletId(session.tes.tl, session.tes.skill)
        }
        case "hard" => {
          db.preestAssign.getHardBookletId(session.tes.tl, session.tes.skill)
        }
      }
    } else if (!session.vsptSubmitted && !session.saSubmitted) {
      // No sa or vspt, request the default assignment.
      db.preestAssign.getMediumBookletId(session.tes.tl, session.tes.skill)
    } else {
      // if either test is done, then we need to get the grade 
      // associated with that test:

      val vsptZScore: Float = if (session.vsptSubmitted) session.vsptZScore else 0.0F

      val saPPE: Float = if (session.saSubmitted) session.saPPE else 0.0F

      if (logger.isDebugEnabled) {
        logger.debug(session.tes.tl + "," + session.tes.skill + "," + session.vsptSubmitted + "," + session.saSubmitted)
      }

      // get the appropriate weight for the given context:
      db.preestWeights.get(session.tes.tl, session.tes.skill, session.vsptSubmitted, session.saSubmitted) match {
        case Some(m: Map[String, Float]) => {
          val pe =  (saPPE * m.get("sa").get) + (vsptZScore * m.get("vspt").get) + m.get("coe").get
          // finaly look up the assignment for the resulting values:
          db.preestAssign.getBookletId(session.tes.tl, session.tes.skill, pe)
        }
        case _ => {
          if (logger.isInfoEnabled) logger.info("No weight returned. The medium booklet will be returned.")
          db.preestAssign.getMediumBookletId(session.tes.tl,session.tes.skill)
        }
      }
    }
  }

  /**
   * Returns the sum of the weights of the questions answered 'true'
   */
  private def getSaRawScore(skill: String, responses: Map[String, Boolean]): Int = {

    responses.keys.foldLeft(0)( (rsc, id) => {

      if (responses.get(id).get) {
        // They responded true to this statement, add its weight.
        db.saWeights.get(skill) match {
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

  def getSaPPEAndLevel(skill: String, responses: Map[String, Boolean]): Tuple2[Float, String] = {

    val rsc = getSaRawScore(skill,responses)
    val levelMap = db.levels
    ((db.saGrades.getPPE(skill,rsc), levelMap.get(db.saGrades.getGrade(skill,rsc)).get))
  }

  /**
   * Used for mcq and gap drop
   */
  def getScoredIdResponseItem(itemId: Int, responseId: Int): Option[ScoredItem] = {

    db.items.get(itemId) match {
        case Some(item) => {
          val scoredItem = new ScoredItem(item.id, item.itemType, item.skill, item.subskill, item.text, item.weight)
          scoredItem.responseId = responseId
          db.getAnswer(responseId) match {
              case Some(answer) => {
                if (answer.correct) {
                  scoredItem.correct = true
                  scoredItem.score = item.weight
                } else {
                  // Score will remain 0
                  scoredItem.correct = false
                }
                Some(scoredItem)
              }
              case None => {
                // The answerId couldn't be found in the system.
                logger.error("No answer with id " + responseId + " was found in the system. Returning None ...")
                None
              }
            }
        }
        case None => {
          // The itemId couldn't be found in the system.
          logger.error("No item with id " + itemId + " was found in the system. Returning None ...")
          None
        }
      }
  }

  /**
   * Used for short answer and gap text
   */
  def getScoredTextResponseItem(itemId:Int,answerText:String): Option[ScoredItem] = {

    db.items.get(itemId) match {
        case Some(item) => {
          //val scoredItem = new ScoredItem(item)
          val scoredItem = new ScoredItem(item.id, item.itemType, item.skill, item.subskill, item.text, item.weight)
          scoredItem.responseText = answerText
          var score = 0
          db.getAnswers(itemId) match {
              case Some(answers) => {
                answers.foreach(correctAnswer => {
                  if (removeWhiteSpaceAndPunctuation(correctAnswer.text).equalsIgnoreCase(removeWhiteSpaceAndPunctuation(answerText))) {
                    scoredItem.score = item.weight;
                    scoredItem.correct = true
                  }
                })
                Some(scoredItem)
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

  def getItemGrade(tl: String, skill: String, bookletId: Int, scoredItems: List[ImmutableItem]): Tuple2[Int, String] = {

    if (logger.isDebugEnabled) logger.debug("NUM ITEMS: " + scoredItems.length)

    val (rawScore, totalWeight) = scoredItems.foldLeft((0,0))( (t, item) => (t._1 + item.score, t._2 + item.weight) )

    if (logger.isDebugEnabled) {
      logger.debug("RAW SCORE: " + rawScore)
      logger.debug("TOTAL WEIGHT: " + totalWeight)
    }

    val itemGrades = db.getItemGrades(tl, skill, bookletId)

    val itemGrade = itemGrades.get(rawScore) match {
        case Some(ig) => ig
        case None => {
          logger.error("No item grade for raw score " + rawScore + ".")
          new ItemGrade(0,0,0)
        }
      }

    ((itemGrade.grade,db.levels.get(itemGrade.grade).get))
  }

  /**
   *  Trim leading and trailing whitespace and then replace tab, newline,
   *  carriage return and form-feed characters with a whitespace.
   */
  private def removeWhiteSpaceAndPunctuation(in: String) = {

    val punctuationList = db.punctuation

    if (punctuationList.size > 0) {
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
