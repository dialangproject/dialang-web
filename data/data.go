package data

import (
	"log"
	"fmt"
	"os"
	"database/sql"
	"slices"

	"dialang.org.uk/web/models"
	_ "github.com/lib/pq"
)

var VSPTBands map[string][]models.VSPTBand
var SAWeights map[string]map[string]int
var SAGrades []models.SAGrade
var PreestWeights map[string]models.PreestWeight
var PreestAssignments map[string][]models.PreestAssignment
var BookletLengths map[int]int
var BookletBaskets map[int][]int

var db *sql.DB

func init() {

	dbHost := os.Getenv("DIALANG_DB_HOST")
	if dbHost == "" {
		dbHost = "dialang-web-db-1"
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
	cacheSAWeights(db)
	cacheSAGrades(db)
	cachePreestWeights(db)
	cachePreestAssignments(db)
	cacheBookletLengths(db)
	cacheBookletBaskets(db)
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
			VSPTBands[band.Locale] = []models.VSPTBand{band}
		} else {
			VSPTBands[band.Locale] = append(bands, band)
		}
	}
}

func cacheSAWeights(db *sql.DB) {

	rows, err := db.Query("SELECT * FROM sa_weights")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	SAWeights = map[string]map[string]int{}
	for rows.Next() {
		var (
			skill string
			wid string
			weight int
		)
		if err := rows.Scan(&skill, &wid, &weight); err != nil {
			log.Fatal(err)
		}
		_, ok := SAWeights[skill]
		if !ok {
			SAWeights[skill] = map[string]int{wid: weight}
		} else {
			SAWeights[skill][wid] = weight
		}
	}
}

func cacheSAGrades(db *sql.DB) {

	rows, err := db.Query("SELECT * FROM sa_grading")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

  	SAGrades = []models.SAGrade{}
	var grade models.SAGrade
	for rows.Next() {
		if err := rows.Scan(&grade.Skill, &grade.Rsc, &grade.Ppe, &grade.Se, &grade.Grade); err != nil {
			log.Fatal(err)
		}
		SAGrades = append(SAGrades, grade)
	}
}

func cachePreestWeights(db *sql.DB) {

	rows, err := db.Query("SELECT * FROM preest_weights")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	var (
		tl string
		skill string
		saTaken int
		vsptTaken int
		weight models.PreestWeight
	)

	PreestWeights = map[string]models.PreestWeight{}

	for rows.Next() {
		if err := rows.Scan(&tl, &skill, &saTaken, &vsptTaken, &weight.Sa, &weight.Vspt, &weight.Coe); err != nil {
			log.Fatal(err)
		}

		key := fmt.Sprintf("%s#%s#%d#%d", tl, skill, saTaken, vsptTaken)
    	PreestWeights[key] = weight
	}
}


func cachePreestAssignments(db *sql.DB) {

	rows, err := db.Query("SELECT * FROM preest_assignments")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	var (
		tl string
		skill string
		assignment models.PreestAssignment
	)

	PreestAssignments = map[string][]models.PreestAssignment{}
	for rows.Next() {
		if err := rows.Scan(&tl, &skill, &assignment.Pe, &assignment.BookletId); err != nil {
			log.Fatal(err)
		}

		key := fmt.Sprintf("%s#%s", tl, skill)
		assignments, ok := PreestAssignments[key]
		if ok {
			PreestAssignments[key] = append(assignments, assignment)
		} else {
			PreestAssignments[key] = []models.PreestAssignment{assignment}
		}
	}

	for k, a := range PreestAssignments {
		slices.SortFunc(a, func(pa1, pa2 models.PreestAssignment) int { return int(pa1.Pe - pa2.Pe) })
		PreestAssignments[k] = a
	}
}

func cacheBookletLengths(db *sql.DB) {

	rows, err := db.Query("SELECT * FROM booklet_lengths")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	BookletLengths = map[int]int{}
	for rows.Next() {
		var bookletId, length int
		if err := rows.Scan(&bookletId, &length); err != nil {
			log.Fatal(err)
		}
		BookletLengths[bookletId] = length
	}
}

func cacheBookletBaskets(db *sql.DB) {

	rows, err := db.Query("SELECT * FROM booklet_basket")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	BookletBaskets = map[int][]int{}
	for rows.Next() {
		var bookletId, basketId int
		if err := rows.Scan(&bookletId, &basketId); err != nil {
			log.Fatal(err)
		}

		basketIds, ok := BookletBaskets[bookletId]
		if ok {
			BookletBaskets[bookletId] = append(basketIds, basketId)
		} else {
			BookletBaskets[bookletId] = []int{basketId}
		}
	}
}
