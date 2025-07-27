import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import {
  DynamoDBDocumentClient,
  PutCommand,
} from "@aws-sdk/lib-dynamodb";
import { randomUUID } from 'crypto'

const client = new DynamoDBClient({});

const dynamo = DynamoDBDocumentClient.from(client);

export const handler = async (event, context) => {

  const responses = {};
  Object.entries(JSON.parse(event.body)).forEach(pair => {
    const name = pair[0];
    if (name.startsWith("word:")) {
      const id = name.split(":")[1]
      const answer = pair[1] === "valid";
      responses[id] = answer;
    }
  });

  console.log(responses);

  /*
  val dialangSession = getDialangSession

  // This is a Tuple3 of zscore, meara score and level.
  val (zScore,mearaScore,level) = vsptUtils.getBand(dialangSession.tes.tl,responses.toMap)

  dialangSession.vsptZScore = zScore.toFloat
  dialangSession.vsptMearaScore = mearaScore
  dialangSession.vsptLevel = level
  dialangSession.vsptSubmitted = true

  logger.debug("VSPT Z Score: " + dialangSession.vsptZScore)
  logger.debug("VSPT Meara Score: " + dialangSession.vsptMearaScore)
  logger.debug("VSPT Level: " + dialangSession.vsptLevel)

  saveDialangSession(dialangSession)

  dataCapture.logVSPTResponses(dialangSession, responses.toMap)
  dataCapture.logVSPTScores(dialangSession)

  if (dialangSession.tes.hideSA && dialangSession.tes.hideTest && dialangSession.resultUrl != "") {
      val url = {
          val parts = dialangSession.resultUrl.split("\\?")
          val params = new StringBuilder(if (parts.length == 2) "?" + parts(1) + "&" else "?")
          params.append("vsptLevel=" + level)
          parts(0) + params.toString
        }
      logger.debug("Redirect URL: " + url)
      contentType = formats("json")
      "{ \"redirect\":\"" + url + "\"}"
    } else {
      contentType = formats("json")
      "{ \"vsptMearaScore\":\"" + mearaScore.toString + "\",\"vsptLevel\":\"" + level + "\",\"vsptDone\": \"true\" }"
    }
  }

  if (!sessionId) {
    // No session yet. Create one.
    sessionId = randomUUID();
    await dynamo.send(
      new PutCommand({
        TableName: "dialang-sessions",
        Item: {
          "session_id": sessionId,
          "ip_address": event.requestContext.identity.sourceIp,
        },
      })
    );
  }

  const passId = randomUUID();

  await dynamo.send(
    new PutCommand({
      TableName: "dialang-passes",
      Item: {
        "session_id": sessionId,
        "pass_id": passId,
        al,
        tl,
        skill,
        started: Date.now(),
      },
    })
  );
  */

  return {
    statusCode: 200,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ "ape": "monkey" }),
  };
};
