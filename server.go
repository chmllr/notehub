package main

import (
	"bytes"
	"html/template"
	"io"
	"io/ioutil"
	"net/http"
	"regexp"
	"strings"
	"time"

	"database/sql"

	_ "github.com/mattn/go-sqlite3"

	"github.com/labstack/echo"
	"github.com/russross/blackfriday"
)

type Template struct{ templates *template.Template }

func (t *Template) Render(w io.Writer, name string, data interface{}, c echo.Context) error {
	return t.templates.ExecuteTemplate(w, name, data)
}

func (t *Template) Lookup(name string) *template.Template {
	return t.templates.Lookup(name)
}

func main() {
	e := echo.New()
	db, err := sql.Open("sqlite3", "./database.sqlite")
	if err != nil {
		e.Logger.Error(err)
	}
	defer db.Close()

	renderer := &Template{templates: template.Must(template.ParseGlob("assets/templates/*.html"))}
	e.Renderer = renderer

	e.File("/favicon.ico", "assets/public/favicon.ico")
	e.File("/robots.txt", "assets/public/robots.txt")
	e.File("/style.css", "assets/public/style.css")
	e.File("/index.html", "assets/public/index.html")
	e.File("/", "assets/public/index.html")

	e.GET("/TOS.md", func(c echo.Context) error {
		n, code := md2html(c, "TOS")
		return c.Render(code, "Page", n)
	})
	e.GET("/:id", func(c echo.Context) error {
		n, code := note(c, db)
		return c.Render(code, "Note", n)
	})
	e.GET("/:id/export", func(c echo.Context) error {
		n, code := note(c, db)
		return c.String(code, n.Text)
	})
	e.GET("/:id/stats", func(c echo.Context) error {
		n, code := note(c, db)
		buf := bytes.NewBuffer([]byte{})
		renderer.Lookup("Stats").Execute(buf, n)
		n.Content = template.HTML(buf.String())
		return c.Render(code, "Note", n)
	})

	e.Logger.Fatal(e.Start(":3000"))
}

type Note struct {
	ID, Title, Text   string
	Published, Edited time.Time
	Views             int
	Content           template.HTML
}

func (n Note) withTitle() Note {
	fstLine := rexpNewLine.Split(n.Text, -1)[0]
	maxLength := 25
	if len(fstLine) < 25 {
		maxLength = len(fstLine)
	}
	n.Title = strings.TrimSpace(rexpNonAlphaNum.ReplaceAllString(fstLine[:maxLength], ""))
	return n
}

var (
	rexpNewLine     = regexp.MustCompile("[\n\r]")
	rexpNonAlphaNum = regexp.MustCompile("[`~!@#$%^&*_|+=?;:'\",.<>{}\\/]")
)

func note(c echo.Context, db *sql.DB) (Note, int) {
	stmt, err := db.Prepare("select id, text, strftime('%s', published) as published," +
		" strftime('%s',edited) as edited, password, views from notes where id = ?")
	if err != nil {
		c.Logger().Error(err)
		return note503, http.StatusServiceUnavailable
	}
	defer stmt.Close()
	row := stmt.QueryRow(c.Param("id"))
	var id, text, password string
	var published, edited int64
	var views int
	if err := row.Scan(&id, &text, &published, &edited, &password, &views); err != nil {
		c.Logger().Error(err)
		return note404, http.StatusNotFound
	}
	return Note{
		ID:        id,
		Text:      text,
		Views:     views,
		Published: time.Unix(published, 0),
		Edited:    time.Unix(edited, 0),
		Content:   mdTmplHTML([]byte(text)),
	}.withTitle(), http.StatusOK
}

func md2html(c echo.Context, name string) (Note, int) {
	path := "assets/markdown/" + name + ".md"
	mdContent, err := ioutil.ReadFile(path)
	if err != nil {
		c.Logger().Errorf("couldn't open markdown page %q: %v", path, err)
		return note503, http.StatusServiceUnavailable
	}
	return Note{Title: name, Content: mdTmplHTML(mdContent)}, http.StatusOK
}

func mdTmplHTML(content []byte) template.HTML { return template.HTML(string(blackfriday.Run(content))) }

// error notes
var note404 = Note{Title: "Not found", Content: mdTmplHTML([]byte("# 404 NOT FOUND"))}
var note503 = Note{Title: "Service unavailable", Content: mdTmplHTML([]byte("# 503 SERVICE UNAVAILABLE"))}
