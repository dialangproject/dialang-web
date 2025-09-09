package data

import (
	"log"
	"os"
	"database/sql"

	"dialang.org.uk/web/models"
	_ "github.com/lib/pq"
)

var VSPTBands map[string][]models.VSPTBand

var db *sql.DB

func init() {

	dbHost := os.Getenv("DIALANG_DB_HOST")
	if dbHost == "" {
		dbHost = "host.docker.internal"
	}

	pw := "e785598fffccc098afda8eb6e42494e5"
	connStr := "postgres://dialangadmin:" + pw + "@" + dbHost + "/dialang?sslmode=disable"
	thisDb, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal(err)
	}
	db = thisDb

	if pingErr := db.Ping(); pingErr != nil {
		log.Fatal(pingErr)
	}

	log.Println("Connected to data database")

	cacheVSPTBands(db)
}

func GetVSPTWords(tl string) ([]models.VSPTWord, error) {

	rows, err := db.Query(`
		SELECT w.* FROM vsp_test_word tw, words w
		WHERE tw.locale = $1 AND tw.word_id = w.word_id`, tl)
	if err != nil {
		return nil, err
	}

	defer rows.Close()

	words := []models.VSPTWord{}
	for rows.Next() {

		var word models.VSPTWord
		if err := rows.Scan(&word.WordId, &word.Word, &word.Valid, &word.Weight); err != nil {
			return nil, err
		}
		words = append(words, word)
	}
	return words, nil
}

func cacheVSPTBands(db *sql.DB) {

	rows, err := db.Query("SELECT * FROM vsp_bands")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	VSPTBands = map[string][]models.VSPTBand{}
	for rows.Next() {
		band := models.VSPTBand{}
		if err := rows.Scan(&band.Locale, &band.Level, &band.Low, &band.High); err != nil {
			log.Fatal(err)
		}
		var bands, ok = VSPTBands[band.Locale]
		if !ok {
			VSPTBands[band.Locale] = []models.VSPTBand{}
		} else {
			VSPTBands[band.Locale] = append(bands, band)
		}
	}
}
