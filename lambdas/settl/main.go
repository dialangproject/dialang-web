package main

import (
	"fmt"
	"context"
	"encoding/json"
	//"time"
	"github.com/aws/aws-lambda-go/events"
	//"github.com/aws/aws-sdk-go-v2/aws"
	//"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-lambda-go/lambda"
	//"github.com/aws/aws-sdk-go-v2/service/dynamodb"
	"github.com/google/uuid"
	//"github.com/aws/aws-sdk-go/aws/session"

)
 type SetTLParams struct {
	 SessionId string `json:"sessionId"`
	 Al string `json:"al"`
	 Tl string `json:"tl"`
	 Skill string `json:"skill"`
 }

var (
	//dbClient *dynamodb.Client
)

func init() {

	/*
	cfg, err := config.LoadDefaultConfig(context.TODO(), config.WithRegion("eu-north-1"))
	if err != nil {
		return
	}

	// Create DynamoDB client
	dbClient = dynamodb.NewFromConfig(cfg)
	*/
}

func handleRequest(ctx context.Context, event events.APIGatewayProxyRequest) {

	var params SetTLParams
	if err := json.Unmarshal([]byte(event.Body), &params); err != nil {
		return
	}

	ipAddress := event.RequestContext.Identity.SourceIP

	if params.SessionId == "" {
		params.SessionId = uuid.NewString()
	}

	passId := uuid.NewString();

	fmt.Sprintf("SessionId: %s, PassId: %s, AL: %s, TL: %s", params.SessionId, passId, params.Al, params.Tl, params.Skill, ipAddress)

	/*
	dbClient.PutItem(ctx, &dynamodb.PutItemInput{
		TableName: aws.String("dialang-data-capture"),
		Item: map[string]*dynamodb.AttributeValue{
			"session_id": {
				"S": aws.String(sessionId),
			},
			"ip_address": {
				"S": aws.String(requestContext["identity"]["sourceIp"]),
			},
			"pass_id": {
				"S": aws.String(passId),
			},
			"al": {
				"S": aws.String(params["al"]),
			},
			"tl": {
				"S": aws.String(params["tl"]),
			},
			"skill": {
				"S": aws.String(params["skill"]),
			},
			"started": {
				"N": aws.String(time.Now().UnixMilli()),
			},
		},
	})
	*/
}

func main() {
	lambda.Start(handleRequest)
}
