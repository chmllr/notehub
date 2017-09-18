package main

import (
	"bytes"
	"crypto/sha256"
	"database/sql"
	"errors"
	"fmt"
	"html/template"
	"math/rand"
	"net/http"
	"regexp"
	"strings"
	"time"

	"github.com/golang-commonmark/markdown"
	"github.com/labstack/echo"
)

func init() {
	rand.Seed(time.Now().UnixNano())
}

const idLength = 5

var (
	errorCodes = map[int]string{
		400: "Bad request",
		401: "Unauthorized",
		404: "Not found",
		412: "Precondition failed",
		503: "Service unavailable",
	}
	rexpNewLine        = regexp.MustCompile("[\n\r]")
	rexpNonAlphaNum    = regexp.MustCompile("[`~!@#$%^&*_|+=?;:'\",.<>{}\\/]")
	rexpNoScriptIframe = regexp.MustCompile("<.*?(iframe|script).*?>")

	errorUnathorised = errors.New("id or password is wrong")
	errorBadRequest  = errors.New("password is empty")
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

func (n *Note) prepare() {
	fstLine := rexpNewLine.Split(n.Text, -1)[0]
	maxLength := 25
	if len(fstLine) < 25 {
		maxLength = len(fstLine)
	}
	n.Text = rexpNoScriptIframe.ReplaceAllString(n.Text, "")
	n.Title = strings.TrimSpace(rexpNonAlphaNum.ReplaceAllString(fstLine[:maxLength], ""))
	n.Content = mdTmplHTML([]byte(n.Text))
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
	return n, tx.Commit()
}

func insert(c echo.Context, db *sql.DB, n *Note) (*Note, error) {
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
			c.Logger().Infof("collision on id %q", id)
			return save(c, db, n)
		}
		return nil, err
	}
	n.ID = id
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
	return *n, http.StatusOK
}

var mdRenderer = markdown.New(markdown.HTML(true))

func mdTmplHTML(content []byte) template.HTML {
	return template.HTML(mdRenderer.RenderToString(content))
}
