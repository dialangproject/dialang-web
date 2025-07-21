import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import {
  DynamoDBDocumentClient,
  ScanCommand,
  PutCommand,
} from "@aws-sdk/lib-dynamodb";
import { randomUUID } from 'crypto'

const client = new DynamoDBClient({});

const dynamo = DynamoDBDocumentClient.from(client);

export const handler = async (event, context) => {

  const sessionId = randomUUID();
  await dynamo.send(
    new PutCommand({
      TableName: "dialang-sessions",
      Item: {
        "session-id": sessionId,
        al: event.queryStringParameters.al || "eng_gb",
      }
    })
  );
  return {
    statusCode: 200,
    headers: { "Content-Type": "text/plain" },
    body: `BALLS: ${sessionId}`,
  };
};
