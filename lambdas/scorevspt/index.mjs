import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import {
  DynamoDBDocumentClient,
  QueryCommand,
  ScanCommand,
  UpdateCommand,
} from "@aws-sdk/lib-dynamodb";
import { randomUUID } from 'crypto'

const client = new DynamoDBClient({});

const docClient = DynamoDBDocumentClient.from(client);

const cacheVSPTBands = async () => {

  const bands = await docClient.send(new ScanCommand({ TableName: "dialang-vspt-bands" }));

  return bands.Items.reduce((acc, band) => {

    const tl = band["test_language"];

    const value = [ band.level, band.low, band.high ];

    if (!acc[tl]) acc[tl] = [];

    acc[tl].push(value);
    return acc;
  }, {});
};

var vsptBands = await cacheVSPTBands();

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

  const [ zScore, mearaScore, level ] = await getBand(body.tl, responses);

  await docClient.send(
    new UpdateCommand({
      TableName: "dialang-data-capture",
      Key: { "session_id": body.sessionId },
      ExpressionAttributeValues: { ":vr": JSON.stringify(responses), ":vs": JSON.stringify([zScore, mearaScore, level]) },
      UpdateExpression: "set vspt_responses_json = :vr, vspt_scores = :vs",
      ReturnValues: "ALL_NEW",
    })
  );

  /*
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

  const result = { zScore, mearaScore, level };

  return {
    statusCode: 200,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(result),
  };
};

var getVersion10ZScore = (hits, realWordsAnswered, falseAlarms, fakeWordsAnswered) => {

  const h =  hits / realWordsAnswered;

  // The false alarm rate. False alarms divided by the total number of fake words answered.
  const f = falseAlarms / fakeWordsAnswered;

  if (h === 1 && f === 1) {
    // This means the test taker has just clicked green for all the words
    return -1;
  } else {
    try {
      const rhs = (( 4 * h * (1 - f) ) - (2 * (h - f) * (1 + h - f))) / ((4 * h * (1 - f)) - ((h - f) * (1 + h - f)));
      return 1 - rhs;
    } catch (ex) {
      return 0;
    }
  }
};

var getZScore = async (tl, responses) => {

  const REAL = 1
  const FAKE = 0
  const yesResponses = [ 0, 0 ]
  const noResponses = [ 0, 0 ]

  const words = await docClient.send(
    new QueryCommand({
      TableName: "dialang-vspt-words",
      KeyConditionExpression: "test_language = :tl",
      ExpressionAttributeValues: { ":tl": tl },
    })
  );

  words.Items.forEach(word => {

    const wordType = word.valid === "1" ?  REAL : FAKE

    if (responses[word.word_id]) {
      yesResponses[wordType] += 1
    } else {
      noResponses[wordType] += 1
    }
  });

  const realWordsAnswered = yesResponses[REAL] + noResponses[REAL];

  const fakeWordsAnswered = yesResponses[FAKE] + noResponses[FAKE];

  // Hits. The number of yes responses to real words.
  const hits = yesResponses[REAL];

  // False alarms. The number of yes responses to fake words.
  const falseAlarms = yesResponses[FAKE];

  if (hits == 0) {
    // No hits whatsoever results in a zero score
    return 0;
  } else {
    return getVersion10ZScore(hits, realWordsAnswered, falseAlarms, fakeWordsAnswered);
  }
};


var getScore = async (tl, responses) => {

  const Z = await getZScore(tl, responses);

  if (Z <= 0) {
    return [Z, 0];
  } else {
    return [Z, Z * 1000];
  }
};

var getBand = async (tl, responses) => {

  const [zScore, mearaScore] = await getScore(tl, responses);

  const bands = vsptBands[tl];

  if (bands) {
    const filtered = bands.filter(b => mearaScore >= b[1] && mearaScore <= b[2]);
    if (filtered.length == 1) {
      return [ zScore, mearaScore, filtered[0][0] ];
    } else {
      console.error(`No level for test language '${tl}' and meara score: ${mearaScore}. Returning empty array ...`);
      return [];
    }
  } else {
    console.error(`No bands for test language '${tl}'. Returning empty array ...`);
    return [];
  }
}
