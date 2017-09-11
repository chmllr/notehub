package main

import (
	"html/template"
	"io"
	"io/ioutil"
	"net/http"
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

func main() {
	e := echo.New()
	db, err := sql.Open("sqlite3", "./database.sqlite")
	if err != nil {
		e.Logger.Error(err)
	}
	defer db.Close()

	e.Renderer = &Template{templates: template.Must(template.ParseGlob("assets/templates/*.html"))}

	e.Static("/", "assets/public")
	e.GET("/TOS.md", func(c echo.Context) error { return c.Render(http.StatusOK, "Page", md2html(c, "TOS")) })
	e.GET("/:id", func(c echo.Context) error { return c.Render(http.StatusOK, "Note", note(c, db)) })

	e.Logger.Fatal(e.Start(":3000"))
}

type Note struct {
	ID, Title, Text   string
	Published, Edited time.Time
	Views             int
	Content           template.HTML
}

func note(c echo.Context, db *sql.DB) Note {
	stmt, err := db.Prepare("select id, text, strftime('%s', published) as published, strftime('%s',edited) as edited, password, views from notes where id = ?")
	if err != nil {
		c.Logger().Error(err)
		return Note{}
	}
	defer stmt.Close()
	row := stmt.QueryRow(c.Param("id"))
	var id, text, password, published, edited string
	var views int
	if err := row.Scan(&id, &text, &published, &edited, &password, &views); err != nil {
		c.Logger().Error(err)
		return Note{} // TODO: use predefined error notes
	}
	// cand := regexp.MustCompile("[\n\r]").Split(text, 1)
	// fmt.Println("CANDIDATE", cand[0])
	return Note{
		ID:      id,
		Content: template.HTML(string(blackfriday.Run([]byte(text)))),
	}
}

func md2html(c echo.Context, name string) Note {
	path := "assets/markdown/" + name + ".md"
	mdContent, err := ioutil.ReadFile(path)
	if err != nil {
		c.Logger().Errorf("couldn't open markdown page %q: %v", path, err)
		return Note{}
	}
	return Note{Title: name, Content: template.HTML(string(blackfriday.Run(mdContent)))}
}
