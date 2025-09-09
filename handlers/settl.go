package handlers

import (
	"encoding/json"
	"net/http"

	"dialang.org.uk/web/utils"
	"dialang.org.uk/web/datacapture"
	"dialang.org.uk/web/models"
	"github.com/labstack/echo/v4"
)

func SetTL(c echo.Context) error {

	req := c.Request()
	decoder := json.NewDecoder(req.Body)
	v := models.SetTLParams{}
	if err := decoder.Decode(&v); err != nil {
		return err
	}

	v.PassId = utils.GenerateUUID()

	v.Referrer = req.Referer()
	v.IPAddress = req.RemoteAddr

	if v.SessionId == "" {
    	v.SessionId = utils.GenerateUUID()
		if err := datacapture.CreateSessionAndPass(&v); err != nil {
			return echo.NewHTTPError(http.StatusInternalServerError, "Failed to create session and pass")
		}
	} else {
		if err := datacapture.CreatePass(&v); err != nil {
			return echo.NewHTTPError(http.StatusInternalServerError, "Failed to create pass")
		}
	}

	return c.JSON(200, map[string]string{
		"passId": v.PassId,
		"sessionId": v.SessionId,
	})
}
