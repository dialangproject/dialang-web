import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import {
  DynamoDBDocumentClient,
  ScanCommand,
} from "@aws-sdk/lib-dynamodb";
import { randomUUID } from 'crypto'

const client = new DynamoDBClient({});

const docClient = DynamoDBDocumentClient.from(client);

var cachePreestWeights = async () => {

  console.debug("Caching pre-estimation weights ...");

  const weights = await docClient.send(new ScanCommand({ TableName: "dialang-preest-weights" }));

  return weights.Items.reduce((acc, weight) => {

    const key = weight.key;
    const vspt = parseFloat(weight.vspt);
    const sa = parseFloat(weight.sa);
    const coe = parseFloat(weight.coe);
    acc[key] = { vspt, sa, coe };
    return acc;
  }, {});
};

var preestWeights = await cachePreestWeights();

console.debug("PREEST WEIGHTS CACHED");
console.debug(preestWeights);

var cachePreestAssign = async () => {

  console.debug("Caching pre-estimation assignments ...");

  const assignments = await docClient.send(new ScanCommand({ TableName: "dialang-preest-assignments" }));

  const temp = assignments.Items.reduce((acc, assignment) => {

    const key = assignment.key;
    const pe = parseFloat(assignment.pe);
    const bookletId = parseInt(assignment.booklet_id);

    acc[key] ??= [];
    acc[key].push({ pe, bookletId });
    return acc;
  }, {});

  Object.keys(temp).forEach(key => temp[key] = temp[key].sort((p1, p2) => p1[0] < p2[0]));
  return temp;
};

var preestAssign = await cachePreestAssign();

console.debug("PREEST ASSIGN CACHED");
console.debug(preestAssign);

export const handler = async (event, context) => {

  const { session } = JSON.parse(event.body);

  session.bookletId = calculateBookletId(session);

  /*
  const bookletLength = getBookletLength(bookletId);

  const firstBasketId = getBasketIdsForBooklet(bookletId);

  console.debug(`BOOKLET ID: ${bookletId}`);
  console.debug(`BOOKLET LENGTH: ${bookletLength}`);
  console.debug(`First Basket Id: ${firstBasketId}`);
  */

  if (!session.id) {
    // No session yet. This could happen in an LTI launch
    session.id = randomUUID();
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
  */

  const response = { session };

  return {
    statusCode: 200,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(response),
  };
};

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
var calculateBookletId = session => {

    if (session.testDifficulty) {
      switch (session.testDifficulty) {
        case "easy":
          //return db.preestAssign.getEasyBookletId(session.tl, session.skill);
          return "easy";
        case "hard":
          //return db.preestAssign.getHardBookletId(session.tl, session.skill);
          return "hard";
        default:
          //return db.preestAssign.getMediumBookletId(session.tl, session.skill);
          return "medium";
      }
    }

    if (!session.vsptSubmitted && !session.saSubmitted) {
      // No sa or vspt, request the default assignment.
      //db.preestAssign.getMediumBookletId(session.tl, session.skill);
      return "medium";
    } else {
      // if either test is done, then we need to get the grade 
      // associated with that test:

      const vsptZScore = session.vsptSubmitted ? session.vsptZScore : 0.0;

      console.debug(`VSPT Z SCORE: ${vsptZScore}`);

      const saPPE = session.saSubmitted ? session.saPPE : 0.0;
      console.debug(`SA PPE: ${saPPE}`);

      const weight = preestWeights[`${session.tl}#${session.skill}#${session.vsptSubmitted}#${session.saSubmitted}`];
      console.debug("WEIGHT");
      console.debug(weight);
      const pe = (saPPE * weight.sa) + (vsptZScore * weight.vspt) + weight.coe;
      console.debug(`PE: ${pe}`);
      const bookletId = preestAssign[`${session.tl}#${session.skill}`].find(t => pe <= t.pe).bookletId;
      console.debug(`Booklet ID: ${bookletId}`);

      return bookletId;

      // get the appropriate weight for the given context:
      /*
      db.preestWeights.get(session.tl, session.skill, session.vsptSubmitted, session.saSubmitted) match {
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
      */
    }
  }

