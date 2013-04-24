package org.dialang.scoring

import org.dialang.common.model.Item
import org.dialang.db.DB
import org.dialang.model.DialangSession

import java.util.StringTokenizer

/**
 *  A utility class intended to hold all of the scoring code used by the system
 *  in one place.
 */
class ScoringMethods {

  val db = DB

  /**
   * Need this for all cases. The assignments tells you which booklet to assign for a given lang/skill/pe.
   */
  val assign = db.getPreestAssign

  /**
	 * and we also need the weights, this gives value to 'weight' the grading model.'s:
   */
  val preestWeights = db.getPreestWeights

  val saWeights = db.getSAWeights

  val saGrades = db.getSAGrades

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

    // collect some facts:

		val tl = session.tl

		val skill = session.skill

		val vsptSubmitted = session.vsptSubmitted

		val saSubmitted = session.saSubmitted

		if (!vsptSubmitted && !saSubmitted) {
			// No sa or vspt, request the default assignment.
			return assign.getBookletId(tl,skill)
		}

		// if either test is done, then we need to get the grade 
		// associated with that test:

		val vsptZScore = if (vsptSubmitted) session.vsptZScore else 0.0F

		val saPPE:Float = if(saSubmitted) session.saPPE else 0.0F

		// get the appropriate weight for the given context:
		val (vsptWeight,saWeight,coe) = preestWeights.get(tl, skill, vsptSubmitted, saSubmitted)

		val pe =  (saPPE * saWeight.asInstanceOf[Float]) + (vsptZScore * vsptWeight) + coe

		// finaly look up the assignment for the resulting values:
		assign.getBookletId(tl,skill,pe)
	}

  /**
   * Returns the sum of the weights of the questions answered 'true'
   */
  def getSaRawScore(skill: String,responses: Map[String,Boolean]):Int = {

    responses.keys.foldLeft(0)( (rsc,id) => {
      if(responses.get(id).get) {
        // They responded true to this statement, add its weight.
        rsc + saWeights.get(skill).get(id)
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
  def getScoredIdResponseItem(itemId:Int,answerId:Int): Option[Item] = {

    val itemOption = db.getItem(itemId)

    if(itemOption.isDefined) {
      val item = itemOption.get
      val answerOption = db.getAnswer(answerId)
      if(answerOption.isDefined) {

        if(answerOption.get.correct) {
          item.correct = true
          item.score = item.weight
        } else {
          item.correct = false
        }
        itemOption
      } else {
        // The answerId couldn't be found in the system.
        None
      }
    } else {
      // The itemId couldn't be found in the system.
      None
    }
  }

  /**
   * Used for short answer and gap text
   */
  def getScoredTextResponseItem(itemId:Int,answerText:String): Option[Item] = {

    val itemOption = db.getItem(itemId)

    if(itemOption.isDefined) {
      val item = itemOption.get
      var score = 0

      val correctAnswersOption = db.getAnswers(itemId)
      if(correctAnswersOption.isDefined) {
        correctAnswersOption.get.foreach(correctAnswer => {
          if(removeWhiteSpaceAndPunctuation(correctAnswer.text).equalsIgnoreCase(removeWhiteSpaceAndPunctuation(answerText))) {
            item.score = item.weight;
            item.correct = true
          }
        })
        itemOption
      } else {
        // No answers for this item
        None
      }
    } else {
      // The itemId couldn't be found in the system.
      None
    }
  }

  def getItemGrade(session:DialangSession,results:List[Item]):Tuple2[Int,String] = {

    var rsc = 0
    var weight = 0

    // results contains the set of results for the entire test - ie the
    // results for each item type.

    results.foreach(item => {

      // total weights:
      weight += item.weight

      // total score:
      rsc += item.score
    })

    val grades = db.getItemGrades(session.tl, session.skill, session.bookletId)

    // normalize:
    rsc = ((rsc.toFloat) * (grades.max / weight.toFloat)).toInt

    val grade = grades.get(rsc)

    ((grade.grade,db.getLevels.get(grade.grade).get))
  }

  /**
   *  Trim leading and trailing whitespace and then replace tab, newline,
   *  carriage return and form-feed characters with a whitespace.
   */
  private def removeWhiteSpaceAndPunctuation(in:String) = {

    val punctuationListOption = db.getPunctuationCharacters

    if(punctuationListOption.isDefined) {

      val punctuationList = punctuationListOption.get

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
