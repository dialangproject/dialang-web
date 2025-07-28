import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import {
  DynamoDBDocumentClient,
  UpdateCommand,
} from "@aws-sdk/lib-dynamodb";
import { randomUUID } from 'crypto'

const client = new DynamoDBClient({});

const docClient = DynamoDBDocumentClient.from(client);

export const handler = async (event, context) => {

  const body = JSON.parse(event.body);

  const responses = {};
  Object.entries(body).forEach(pair => {
    const name = pair[0];
    if (name.startsWith("word:")) {
      const id = name.split(":")[1]
      const answer = pair[1] === "valid";
      responses[id] = answer;
    }
  });

  await docClient.send(
    new UpdateCommand({
      TableName: "dialang-data-capture",
      Key: { "session_id": body.sessionId },
      ExpressionAttributeValues: { ":vr": JSON.stringify(responses) },
      UpdateExpression: "set vspt_responses_json = :vr",
      ReturnValues: "ALL_NEW",
    })
  );
  /*
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
  */

  const mearaScore = 905;
  const level = "UNKNOWN";

  return {
    statusCode: 200,
    headers: { "Content-Type": "application/json" },
    body: `{ "vsptMearaScore": ${mearaScore}, "vsptLevel": "${level}" }`,
  };
};
