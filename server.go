package main

import (
	"bytes"
	"html/template"
	"io"
	"io/ioutil"
	"math"
	"net/http"
	"net/url"
	"os"
	"sync"

	"database/sql"

	_ "github.com/mattn/go-sqlite3"

	"github.com/labstack/echo"
	"github.com/labstack/gommon/log"
)

var (
	stats = &sync.Map{}
	ads   []byte
)

type Template struct{ templates *template.Template }

func (t *Template) Render(w io.Writer, name string, data interface{}, c echo.Context) error {
	return t.templates.ExecuteTemplate(w, name, data)
}

func main() {
	e := echo.New()
	e.Logger.SetLevel(log.DEBUG)

	db, err := sql.Open("sqlite3", "./database.sqlite")
	if err != nil {
		e.Logger.Error(err)
	}
	defer db.Close()

	adsFName := os.Getenv("ADS")
	if adsFName != "" {
		var err error
		ads, err = ioutil.ReadFile(adsFName)
		if err != nil {
			e.Logger.Errorf("couldn't read file %q: %v", adsFName, err)
		}
	}

	e.Renderer = &Template{templates: template.Must(template.ParseGlob("assets/templates/*.html"))}

	e.File("/favicon.ico", "assets/public/favicon.ico")
	e.File("/robots.txt", "assets/public/robots.txt")
	e.File("/style.css", "assets/public/style.css")
	e.File("/index.html", "assets/public/index.html")
	e.File("/", "assets/public/index.html")

	go persistStats(e.Logger, db, stats)

	e.GET("/TOS.md", func(c echo.Context) error {
		n, code := md2html(c, "TOS")
		return c.Render(code, "Page", n)
	})

	e.GET("/:id", func(c echo.Context) error {
		n, code := load(c, db)
		n.prepare()
		views := n.Views
		if val, ok := stats.Load(n.ID); ok {
			intVal, ok := val.(int)
			if ok {
				views = intVal
			}
		}
		defer stats.Store(n.ID, views+1)
		if fraudelent(n) {
			n.Ads = mdTmplHTML(ads)
		}
		c.Logger().Debugf("/%q requested; response code: %d", n.ID, code)
		return c.Render(code, "Note", n)
	})

	e.GET("/:id/export", func(c echo.Context) error {
		n, code := load(c, db)
		c.Logger().Debugf("/%q/export requested; response code: %d", n.ID, code)
		return c.String(code, n.Text)
	})

	e.GET("/:id/stats", func(c echo.Context) error {
		n, code := load(c, db)
		n.prepare()
		buf := bytes.NewBuffer([]byte{})
		e.Renderer.Render(buf, "Stats", n, c)
		n.Content = template.HTML(buf.String())
		c.Logger().Debugf("/%q/stats requested; response code: %d", n.ID, code)
		return c.Render(code, "Note", n)
	})

	e.GET("/:id/edit", func(c echo.Context) error {
		n, code := load(c, db)
		c.Logger().Debugf("/%q/edit requested; response code: %d", n.ID, code)
		return c.Render(code, "Form", n)
	})

	e.GET("/new", func(c echo.Context) error {
		c.Logger().Debug("/new requested")
		return c.Render(http.StatusOK, "Form", nil)
	})

	e.POST("/note", func(c echo.Context) error {
		c.Logger().Debug("POST /note requested")
		vals, err := c.FormParams()
		if err != nil {
			return err
		}
		if get(vals, "tos") != "on" {
			code := http.StatusPreconditionFailed
			c.Logger().Errorf("POST /note error: %d", code)
			return c.Render(code, "Note", errPage(code))
		}
		text := get(vals, "text")
		if 10 > len(text) || len(text) > 50000 {
			code := http.StatusBadRequest
			c.Logger().Errorf("POST /note error: %d", code)
			return c.Render(code, "Note",
				errPage(code, "note length not accepted"))
		}
		id := get(vals, "id")
		n := &Note{
			ID:       id,
			Text:     text,
			Password: get(vals, "password"),
		}
		n, err = save(c, db, n)
		if err != nil {
			c.Logger().Error(err)
			code := http.StatusServiceUnavailable
			if err == errorUnathorised {
				code = http.StatusUnauthorized
			} else if err == errorBadRequest {
				code = http.StatusBadRequest
			}
			c.Logger().Errorf("POST /note error: %d", code)
			return c.Render(code, "Note", errPage(code, err.Error()))
		}
		c.Logger().Debugf("note %q saved", n.ID)
		return c.Redirect(http.StatusMovedPermanently, "/"+n.ID)
	})

	e.Logger.Fatal(e.Start(":3000"))
}

func get(vals url.Values, key string) string {
	if list, found := vals[key]; found {
		if len(list) == 1 {
			return list[0]
		}
	}
	return ""
}

func fraudelent(n *Note) bool {
	stripped := rexpLink.ReplaceAllString(n.Text, "")
	l1 := len(n.Text)
	l2 := len(stripped)
	return n.Views > 100 &&
		int(math.Ceil(100*float64(l1-l2)/float64(l1))) > fraudThreshold
}
