import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import {
  DynamoDBDocumentClient,
  PutCommand,
} from "@aws-sdk/lib-dynamodb";
import { randomUUID } from 'crypto'

const client = new DynamoDBClient({});

const dynamo = DynamoDBDocumentClient.from(client);

export const handler = async (event, context) => {

  let { sessionId, al, tl, skill } = JSON.parse(event.body);

  if (!sessionId) {
    // No session yet. Create one.
    sessionId = randomUUID();
  }

  const passId = randomUUID();

  await dynamo.send(
    new PutCommand({
      TableName: "dialang-data-capture",
      Item: {
        "session_id": sessionId,
        "ip_address": event.requestContext.identity.sourceIp,
        "pass_id": passId,
        al,
        tl,
        skill,
        started: Date.now(),
        vspt_responses_json: "",
      },
    })
  );

  return {
    statusCode: 200,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ sessionId, passId }),
  };
};
