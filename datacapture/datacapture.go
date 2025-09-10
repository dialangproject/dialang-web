package datacapture

import (
	"log"
	"os"
	"database/sql"
	"time"

	"dialang.org.uk/web/models"
	_ "github.com/lib/pq"
)

var db *sql.DB

func init() {

	dbHost := os.Getenv("DIALANG_DB_HOST")
	if dbHost == "" {
		dbHost = "dialang-web-db-1"
	}

	pw := "e785598fffccc098afda8eb6e42494e5"
	connStr := "postgres://dialangadmin:" + pw + "@" + dbHost + "/dialang-data-capture?sslmode=disable"
	thisDb, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal(err)
	}

	db = thisDb

	if pingErr := db.Ping(); pingErr != nil {
		log.Fatal(pingErr)
	}

	log.Println("Connected to data capture database")
}

func CreateSessionAndPass(v *models.SetTLParams) error {

	now := time.Now().UnixMilli()

	if _, err := db.Exec("INSERT INTO sessions (id, ip_address, started, browser_locale, referrer) values($1, $2, $3, $4, $5)",
				v.SessionId,
				v.IPAddress,
				now,
				v.BrowserLocale,
				v.Referrer); err != nil {
		log.Println(err)
		return err
	}

	if err := CreatePass(v); err != nil {
		log.Println(err)
		return err
	}

	return nil
}

func CreatePass(v *models.SetTLParams) error {

	now := time.Now().UnixMilli()

	if _, err := db.Exec("INSERT INTO passes (id, session_id, al, tl, skill, started) values($1, $2, $3, $4, $5, $6)",
				v.PassId,
				v.SessionId,
				v.Al,
				v.Tl,
				v.Skill,
				now); err != nil {

		log.Println(err)
		return err
	}

	return nil
}
