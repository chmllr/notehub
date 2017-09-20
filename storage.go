package main

import (
	"bytes"
	"crypto/sha256"
	"database/sql"
	"fmt"
	"html/template"
	"math/rand"
	"net/http"
	"strings"
	"time"

	"github.com/labstack/echo"
)

func init() {
	rand.Seed(time.Now().UnixNano())
}

const (
	idLength       = 5
	fraudThreshold = 7
)

type Note struct {
	ID, Title, Text, Password string
	Published, Edited         time.Time
	Views                     int
	Content, Ads              template.HTML
}

func save(c echo.Context, db *sql.DB, n *Note) (*Note, error) {
	if n.Password != "" {
		n.Password = fmt.Sprintf("%x", sha256.Sum256([]byte(n.Password)))
	}
	if n.ID == "" {
		return insert(c, db, n)
	}
	return update(c, db, n)
}

func update(c echo.Context, db *sql.DB, n *Note) (*Note, error) {
	c.Logger().Debugf("updating note %s", n.ID)
	if n.Password == "" {
		return nil, errorBadRequest
	}
	tx, err := db.Begin()
	if err != nil {
		return nil, err
	}
	stmt, _ := tx.Prepare("update notes set (text, edited) = (?, ?) where id = ? and password = ?")
	defer stmt.Close()
	res, err := stmt.Exec(n.Text, time.Now(), n.ID, n.Password)
	if err != nil {
		tx.Rollback()
		return nil, err
	}
	rows, err := res.RowsAffected()
	if rows != 1 {
		tx.Rollback()
		return nil, errorUnathorised
	}
	c.Logger().Debugf("updating note %s; committing transaction", n.ID)
	return n, tx.Commit()
}

func insert(c echo.Context, db *sql.DB, n *Note) (*Note, error) {
	c.Logger().Debug("inserting new note")
	tx, err := db.Begin()
	if err != nil {
		return nil, err
	}
	stmt, _ := tx.Prepare("insert into notes(id, text, password) values(?, ?, ?)")
	defer stmt.Close()
	id := randId()
	_, err = stmt.Exec(id, n.Text, n.Password)
	if err != nil {
		tx.Rollback()
		if strings.HasPrefix(err.Error(), "UNIQUE constraint failed") {
			c.Logger().Infof("collision on id %s", id)
			return save(c, db, n)
		}
		return nil, err
	}
	n.ID = id
	c.Logger().Debugf("inserting new note %s; commiting transaction", n.ID)
	return n, tx.Commit()
}

func randId() string {
	buf := bytes.NewBuffer([]byte{})
	for i := 0; i < idLength; i++ {
		b := '0'
		z := rand.Intn(36)
		if z > 9 {
			b = 'a'
			z -= 10
		}
		buf.WriteRune(rune(z) + b)
	}
	return buf.String()
}

func load(c echo.Context, db *sql.DB) (*Note, int) {
	q := c.Param("id")
	c.Logger().Debugf("loading note %s", q)
	stmt, _ := db.Prepare("select * from notes where id = ?")
	defer stmt.Close()
	row := stmt.QueryRow(q)
	var id, text, password string
	var published time.Time
	var editedVal interface{}
	var views int
	if err := row.Scan(&id, &text, &published, &editedVal, &password, &views); err != nil {
		c.Logger().Error(err)
		code := http.StatusNotFound
		return responsePage(code), code
	}
	n := &Note{
		ID:        id,
		Text:      text,
		Views:     views,
		Published: published,
	}
	if editedVal != nil {
		n.Edited = editedVal.(time.Time)
	}
	return n, http.StatusOK
}
