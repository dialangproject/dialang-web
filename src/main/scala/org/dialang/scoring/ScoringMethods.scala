package org.dialang.scoring

import org.dialang.db.DB
import org.dialang.model.DialangSession

/**
 *  A utility class intended to hold all of the scoring code used by the system
 *  in one place.
 */
class ScoringMethods {

  val db = new DB

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

  def getSaPPE(skill: String,responses: Map[String,Boolean]):Float = {
    val rsc = getSaRawScore(skill,responses)
    saGrades.getPPE(skill,rsc)
  }
}
