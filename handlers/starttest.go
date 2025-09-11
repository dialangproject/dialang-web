package handlers

import (
	"encoding/json"
	"fmt"
	"log"
	"dialang.org.uk/web/data"
	"dialang.org.uk/web/models"
	"dialang.org.uk/web/utils"
	"github.com/labstack/echo/v4"
)

func StartTest(c echo.Context) error {

	decoder := json.NewDecoder(c.Request().Body)
	session := models.Session{}
	if err := decoder.Decode(&session); err != nil {
		log.Println(err)
		return err
	}

  	session.BookletId = calculateBookletId(&session)

	log.Printf("BOOKLET ID: %d\n", session.BookletId)

	bookletLength := data.BookletLengths[session.BookletId]
	log.Println(bookletLength)
	firstBasketId := data.BookletBaskets[session.BookletId][0]
	log.Println(firstBasketId)

  /*
  const firstBasketId = getBasketIdsForBooklet(bookletId);

  console.debug(`BOOKLET ID: ${bookletId}`);
  console.debug(`BOOKLET LENGTH: ${bookletLength}`);
  console.debug(`First Basket Id: ${firstBasketId}`);
  */

	if session.Id == "" {
		// No session yet. This could happen in an LTI launch
		session.Id = utils.GenerateUUID()
	}

  //dataCapture.logTestStart(dialangSession.passId, dialangSession.bookletId, dialangSession.bookletLength)
  //
  /*
  await docClient.send(
    new UpdateCommand({
      TableName: "dialang-data-capture",
      Key: { "session_id": body.sessionId },
      ExpressionAttributeValues: { ":vr": JSON.stringify(responses), ":vs": JSON.stringify([zScore, mearaScore, level]) },
      UpdateExpression: "set vspt_responses_json = :vr, vspt_scores = :vs",
      ReturnValues: "ALL_NEW",
    })
  );

  const response = { session };

  return {
    statusCode: 200,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(response),
  };
  */
  return c.JSON(200, session)
}

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
func calculateBookletId(session *models.Session) int {

	key := fmt.Sprintf("%s#%s", session.Tl, session.Skill)

	if session.TestDifficulty != "" {
      	switch session.TestDifficulty {
        	case "easy":
          		return data.PreestAssignments[key][0].BookletId
        	case "hard":
          		return data.PreestAssignments[key][2].BookletId
        	default:
          		return data.PreestAssignments[key][1].BookletId
		}
	}

    if session.VsptSubmitted == 0 && session.SaSubmitted == 0 {
		log.Println("No vsp or sa submitted")
      	// No sa or vspt, request the default assignment.
      	return data.PreestAssignments[key][1].BookletId
    } else {
		log.Println("vsp or sa submitted")
      // if either test is done, then we need to get the grade 
      // associated with that test:

      var vsptZScore, saPPE  float64
	  if session.VsptSubmitted == 1 {
		  vsptZScore = session.VsptZScore
		  log.Printf("VSPT SUBMITTED. vsptZScore: %f\n", vsptZScore)
	  }
      if session.SaSubmitted == 1 {
		  saPPE = session.SaPPE
		  log.Printf("SA SUBMITTED. saPPE: %f\n", saPPE)
	  }
	  weightKey := fmt.Sprintf("%s#%d#%d", key, session.VsptSubmitted, session.SaSubmitted)
	  weight := data.PreestWeights[weightKey]
	  pe := (saPPE * weight.Sa) + (vsptZScore * weight.Vspt) + weight.Coe

	  log.Printf("PE: %f\n", pe)

	  log.Println(key)
	  log.Println(data.PreestAssignments[key])

	  var bookletId int
	  for _, ass := range data.PreestAssignments[key] {
		  fmt.Println(ass.Pe)
      	if pe <= ass.Pe {
			bookletId = ass.BookletId
			break;
		}
	  }
	  return bookletId
    }
  }
