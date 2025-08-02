import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import {
  DynamoDBDocumentClient,
  QueryCommand,
  ScanCommand,
  UpdateCommand,
} from "@aws-sdk/lib-dynamodb";

const client = new DynamoDBClient({});

const docClient = DynamoDBDocumentClient.from(client);

var cefrLevels = { 1: "A1", 2: "A2", 3: "B1", 4: "B2", 5: "C1", 6: "C2" };

const cacheSAWeights = async () => {

  const weights = await docClient.send(new ScanCommand({ TableName: "dialang-sa-weights" }));

  return weights.Items.reduce((map, weight) => {

    const skill = weight["skill"];

    if (!map[skill]) map[skill] = {};
    map[skill][weight.wid] =  weight["weight"];
    return map;
  }, {});
};

var saWeights = await cacheSAWeights();

const cacheSAGrades = async () => {

  const grades = await docClient.send(new ScanCommand({ TableName: "dialang-sa-grading" }));

  return grades.Items.map(grade => {
    return {
      skill: grade["skill"],
      rsc: grade["rsc"],
      ppe: grade["ppe"],
      se: grade["se"],
      grade: grade["grade"],
    };
  });
};

var saGrades = await cacheSAGrades();

export const handler = async (event, context) => {

  const body = JSON.parse(event.body);

  const responses = {};
  Object.entries(body).forEach(pair => {
    const name = pair[0];
    if (name.startsWith("statement:")) {
      const wid = name.split(":")[1]
      const answer = pair[1] === "yes";
      responses[wid] = answer;
    }
  });

  const [saPPE, saLevel] = getSaPPEAndLevel(body.skill, responses);

  await docClient.send(
    new UpdateCommand({
      TableName: "dialang-data-capture",
      Key: { "session_id": body.sessionId },
      ExpressionAttributeValues: { ":sar": JSON.stringify(responses), ":sal": saLevel, ":sap": saPPE },
      UpdateExpression: "set sa_responses_json = :sar, sa_level = :sal, sa_ppe = :sap",
      ReturnValues: "ALL_NEW",
    })
  );

  const result = { saPPE, saLevel };

  return {
    statusCode: 200,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(result),
  };
};

/**
   * Returns the sum of the weights of the questions answered 'true'
   */
var getSaRawScore = (skill, responses) => {

  return Object.keys(responses).reduce((acc, id) => {

    if (responses[id]) {
      // They responded true to this statement, add its weight.
      const wordMap = saWeights[skill];
      return acc + parseInt(wordMap[id]);
    } else {
      return acc
    }
  }, 0);
};

var getSaPPEAndLevel = (skill, responses) => {

  const rsc = getSaRawScore(skill, responses);
  const grade = saGrades.find(g => g.skill === skill && g.rsc === rsc);
  return [grade.ppe, grade.grade];
};
