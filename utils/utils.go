package utils

import (
	"log"
	"github.com/google/uuid"
)

func GenerateUUID() string {

	id, err := uuid.NewV7()
	if err != nil {
		log.Println(err)
		return ""
	}

	return id.String()
}
