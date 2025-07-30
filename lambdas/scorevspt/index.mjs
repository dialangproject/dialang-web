import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import {
  DynamoDBDocumentClient,
  QueryCommand,
  UpdateCommand,
} from "@aws-sdk/lib-dynamodb";
import { randomUUID } from 'crypto'

const client = new DynamoDBClient({});

const docClient = DynamoDBDocumentClient.from(client);

val cacheVSPTBands = () => {

  const bands = await docClient.send(
    new QueryCommand({
      TableName: "dialang-content",
      KeyConditionExpression: "pk = :pk",
      ExpressionAttributeValues: { ":pk": "vspt_band" },
      ConsistentRead: true,
    })
  );

  bands.forEach(band => {
  });

  debug("Caching VSPT bands ...")

  lazy val conn = ds.getConnection
  lazy val st = conn.createStatement

  try {
    var rs = st.executeQuery("SELECT * FROM vsp_bands")

    val temp = rs.foldLeft(new HashMap[String, ArrayBuffer[(String, Int, Int)]]())((map,r) => {
        val locale = r.getString("locale")
        val level = r.getString("level")
        val low = r.getInt("low")
        val high = r.getInt("high")
        if (!map.contains(locale)) {
          map += (locale -> new ArrayBuffer[(String, Int, Int)])
        }
        map.get(locale).get += ((level,low,high))
        map
      }).toMap

    // Make it all immutable and return it
    temp.map(t => (t._1 -> t._2.toVector)).toMap
  } finally {
    if (st != null) {
      try {
        st.close()
      } catch { case e:SQLException => }
    }

    if (conn != null) {
      try {
        conn.close()
      } catch { case e:SQLException => }
    }

    debug("VSPT bands cached.")
  }
}


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

var getVersion10ZScore = (hits, realWordsAnswered, falseAlarms, fakeWordsAnswered) => {

  //require(hits >= 0 && realWordsAnswered > 0 && falseAlarms >= 0 && fakeWordsAnswered > 0
   //         , "One of these conditions failed. hits >= 0, realWordsAnswered > 0, falseAlarms >= 0, fakeWordsAnswered > 0")

  // The observed hit rate. Hits divided by the  total number of real words answered.
  //const h =  hits.toDouble / realWordsAnswered.toDouble
  const h =  hits / realWordsAnswered;

  // The false alarm rate. False alarms divided by the total number of fake words answered.
  const f = falseAlarms / fakeWordsAnswered;

  if (h === 1 && f === 1) {
    // This means the test taker has just clicked green for all the words
    return -1
  } else {
    try {
      const rhs = (( 4 * h * (1 - f) ) - (2 * (h - f) * (1 + h - f))) / ((4 * h * (1 - f)) - ((h - f) * (1 + h - f)));
      return 1 - rhs;
    } catch (ex) {
      return 0;
    }
  }
};

var getZScore = (tl, responses) => {

  const REAL = 1
  const FAKE = 0
  const yesResponses = [ 0, 0 ]
  const noResponses = [ 0, 0 ]

  const words = await docClient.send(
    new QueryCommand({
      TableName: "dialang-content",
      Key: { "pk": "vspt_word", "sk": tl },
    })
  );

  words.forEach(word => {

    const wordType = word.valid ?  REAL : FAKE

    if (responses.contains(word.id)) {
      if (responses(word.id)) {
        yesResponses[wordType] += 1
      } else {
        noResponses[wordType] += 1
      }
    } else {
      console.error(`The responses did not contain the word with id '${word.id}'. This is incorrect.`)
    }
  });

  const realWordsAnswered = yesResponses[REAL] + noResponses[REAL]
  console.debug(`realWordsAnswered: ${realWordsAnswered}`)

  const fakeWordsAnswered = yesResponses[FAKE] + noResponses[FAKE]
  console.debug(`fakeWordsAnswered: ${fakeWordsAnswered}`)

  // Hits. The number of yes responses to real words.
  const hits = yesResponses[REAL]
  console.debug(`hits: ${hits}`);

  // False alarms. The number of yes responses to fake words.
  const falseAlarms = yesResponses[FAKE]
  console.debug(`falseAlarms: ${falseAlarms}`)

  if (hits == 0) {
    // No hits whatsoever results in a zero score
    return 0;
  } else {
    return getVersion10ZScore(hits, realWordsAnswered, falseAlarms, fakeWordsAnswered);
  }
};


var getScore = (tl, responses) => {

  const Z = getZScore(tl, responses);

  if (Z <= 0) {
    return ((Z,0));
  } else {
    return ((Z,(Z * 1000)));
  }
};

var getBand = (tl, responses) => {

  const val (zScore,mearaScore) = getScore(tl,responses);

  console.debug(`zScore: ${zScore}. mearaScore: ${mearaScore}`)

  val level = db.vsptBands.get(tl) match {
      case Some(band: Vector[(String, Int, Int)]) => {
        val filtered = band.filter(t => mearaScore >= t._2 && mearaScore <= t._3)
        if(filtered.length == 1) {
          filtered(0)._1
        } else {
          logger.error("No level for test language '" + tl + "' and meara score: " + mearaScore + ". Returning UNKNOWN ...")
          "UNKNOWN"
        }
      }
    }

  console.debug(`Level: ${level}`)

  return ((zScore,mearaScore,level));
}

