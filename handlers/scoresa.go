package handlers

import (
	"encoding/json"
	"errors"
	"strings"
	"net/http"
	//"fmt"
	"log"
	"dialang.org.uk/web/data"
	//"dialang.org.uk/web/models"
	"github.com/labstack/echo/v4"
)

var CEFR_LEVELS = map[int]string{ 1: "A1", 2: "A2", 3: "B1", 4: "B2", 5: "C1", 6: "C2" }

func ScoreSA(c echo.Context) error {

	decoder := json.NewDecoder(c.Request().Body)
	var saResponses map[string]string
	if err := decoder.Decode(&saResponses); err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, "Failed to decode JSON body")
	}

	skill, ok := saResponses["skill"]
	if !ok {
		return echo.NewHTTPError(http.StatusBadRequest, "No skill supplied in JSON body")
	}

  	responses := map[string]bool{}
	for name, v := range saResponses {
		if strings.HasPrefix(name, "statement:") {
			wid := strings.Split(name, ":")[1]
			responses[wid] = v == "yes"
		}
	}

	log.Println(responses)

	saPPE, saLevel, err := getSaPPEAndLevel(skill, responses);

	if err != nil {
		log.Printf("Failed to score self assessment for skill %s\n", skill)
		return echo.NewHTTPError(http.StatusInternalServerError, "Failed to create score SA")
	}

	return c.JSON(200, map[string]any{"saPPE": saPPE, "saLevel": saLevel})

	/*
	await docClient.send(
		new UpdateCommand({
		  TableName: "dialang-data-capture",
		  Key: { "session_id": body.sessionId },
		  ExpressionAttributeValues: { ":sar": JSON.stringify(responses), ":sal": saLevel, ":sap": saPPE },
		  UpdateExpression: "set sa_responses_json = :sar, sa_level = :sal, sa_ppe = :sap",
		  ReturnValues: "ALL_NEW",
		})
	);
    */
}

func getSaRawScore(skill string, responses map[string]bool) int {

	var wordMap map[string]int = data.SAWeights[skill]

	var score int

	for id, response := range responses {
		if response {
			// They responded true to this statement, add its weight.
			score += wordMap[id]
		}
	}

	return score
}

func getSaPPEAndLevel(skill string, responses map[string]bool) (float64, int, error) {

	rsc := getSaRawScore(skill, responses)
	for _, g := range data.SAGrades {
		if g.Skill == skill && g.Rsc == rsc {
			return g.Ppe, g.Grade, nil
		}
	}

	return 0, 0, errors.New("Failed to match skill and raw score to an sa grade")
}
