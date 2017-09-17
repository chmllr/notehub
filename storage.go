package main

import (
	"bytes"
	"database/sql"
	"fmt"
	"html/template"
	"math/rand"
	"net/http"
	"regexp"
	"strings"
	"time"

	"github.com/labstack/echo"
	"github.com/russross/blackfriday"
)

func init() {
	rand.Seed(time.Now().UnixNano())
}

const idLength = 5

var (
	errorCodes = map[int]string{
		400: "Bad request",
		404: "Not found",
		412: "Precondition failed",
		503: "Service unavailable",
	}
	rexpNewLine     = regexp.MustCompile("[\n\r]")
	rexpNonAlphaNum = regexp.MustCompile("[`~!@#$%^&*_|+=?;:'\",.<>{}\\/]")
)

type Note struct {
	ID, Title, Text, Password string
	Published, Edited         time.Time
	Views                     int
	Content                   template.HTML
}

func errPage(code int, details ...string) Note {
	text := errorCodes[code]
	if len(details) > 0 {
		text += ": " + strings.Join(details, ";")
	}
	return Note{
		Title:   text,
		Content: mdTmplHTML([]byte(fmt.Sprintf("# %d %s", code, text))),
	}
}

func (n *Note) render() {
	fstLine := rexpNewLine.Split(n.Text, -1)[0]
	maxLength := 25
	if len(fstLine) < 25 {
		maxLength = len(fstLine)
	}
	n.Title = strings.TrimSpace(rexpNonAlphaNum.ReplaceAllString(fstLine[:maxLength], ""))
	n.Password = ""
	n.Content = mdTmplHTML([]byte(n.Text))
}

func save(c echo.Context, db *sql.DB, n *Note) (string, error) {
	tx, err := db.Begin()
	if err != nil {
		return "", err
	}
	stmt, _ := tx.Prepare("insert into notes(id, text, password) values(?, ?, ?)")
	defer stmt.Close()
	id := randId()
	_, err = stmt.Exec(id, n.Text, n.Password)
	if err != nil {
		if strings.HasPrefix(err.Error(), "UNIQUE constraint failed") {
			tx.Rollback()
			c.Logger().Infof("collision on id %q", id)
			return save(c, db, n)
		}
		return "", err
	}
	return id, tx.Commit()
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

func load(c echo.Context, db *sql.DB) (Note, int) {
	stmt, _ := db.Prepare("select * from notes where id = ?")
	defer stmt.Close()
	row := stmt.QueryRow(c.Param("id"))
	var id, text, password string
	var published time.Time
	var editedVal interface{}
	var views int
	if err := row.Scan(&id, &text, &published, &editedVal, &password, &views); err != nil {
		c.Logger().Error(err)
		code := http.StatusNotFound
		return errPage(code), code
	}
	var edited time.Time
	if editedVal != nil {
		edited = editedVal.(time.Time)
	}
	n := &Note{
		ID:        id,
		Text:      text,
		Views:     views,
		Published: published,
		Edited:    edited,
	}
	n.render()
	return *n, http.StatusOK
}

func mdTmplHTML(content []byte) template.HTML {
	return template.HTML(string(blackfriday.Run(content)))
}
