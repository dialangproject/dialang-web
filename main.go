package main

import (
	"net/http"

	"dialang.org.uk/web/handlers"
	"github.com/labstack/echo/v4"
)

func main()  {

	e := echo.New()
	e.GET("/", func(c echo.Context) error {
		return c.String(http.StatusOK, "Hello, World!")
	})
	e.POST("/settl", handlers.SetTL)
	e.POST("/scorevspt", handlers.ScoreVSPT)
	e.Logger.Fatal(e.Start(":8080"))
}
