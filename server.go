package main

import (
	"bytes"
	"html/template"
	"io"
	"io/ioutil"
	"math"
	"net/http"
	"os"
	"sync"
	"time"

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
			e.Logger.Errorf("couldn't read file %s: %v", adsFName, err)
		}
	}

	go persistStats(e.Logger, db, stats)
	go cleanAccessRegistry(e.Logger)

	e.Renderer = &Template{templates: template.Must(template.ParseGlob("assets/templates/*.html"))}

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
		c.Logger().Debugf("/%s requested; response code: %d", n.ID, code)
		return c.Render(code, "Note", n)
	})

	e.GET("/:id/export", func(c echo.Context) error {
		n, code := load(c, db)
		c.Logger().Debugf("/%s/export requested; response code: %d", n.ID, code)
		return c.String(code, n.Text)
	})

	e.GET("/:id/stats", func(c echo.Context) error {
		n, code := load(c, db)
		n.prepare()
		buf := bytes.NewBuffer([]byte{})
		e.Renderer.Render(buf, "Stats", n, c)
		n.Content = template.HTML(buf.String())
		c.Logger().Debugf("/%s/stats requested; response code: %d", n.ID, code)
		return c.Render(code, "Note", n)
	})

	e.GET("/:id/edit", func(c echo.Context) error {
		n, code := load(c, db)
		c.Logger().Debugf("/%s/edit requested; response code: %d", n.ID, code)
		return c.Render(code, "Form", n)
	})

	e.GET("/new", func(c echo.Context) error {
		c.Logger().Debug("/new requested")
		return c.Render(http.StatusOK, "Form", nil)
	})

	e.POST("/note", func(c echo.Context) error {
		c.Logger().Debug("POST /note requested")
		if !legitAccess(c) {
			code := http.StatusTooManyRequests
			c.Logger().Errorf("rate limit exceeded for %s", c.Request().RemoteAddr)
			return c.Render(code, "Note", responsePage(code))
		}
		if c.FormValue("tos") != "on" {
			code := http.StatusPreconditionFailed
			c.Logger().Errorf("POST /note error: %d", code)
			return c.Render(code, "Note", responsePage(code))
		}
		id := c.FormValue("id")
		text := c.FormValue("text")
		l := len(text)
		if (id == "" || id != "" && l != 0) && (10 > l || l > 50000) {
			code := http.StatusBadRequest
			c.Logger().Errorf("POST /note error: %d", code)
			return c.Render(code, "Note",
				responsePage(code, "note length not accepted"))
		}
		n := &Note{
			ID:       id,
			Text:     text,
			Password: c.FormValue("password"),
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
			return c.Render(code, "Note", responsePage(code, err.Error()))
		}
		c.Logger().Debugf("note %s saved", n.ID)
		return c.Redirect(http.StatusMovedPermanently, "/"+n.ID)
	})

	e.POST("/:id/report", func(c echo.Context) error {
		report := c.FormValue("report")
		if legitAccess(c) && report != "" {
			id := c.Param("id")
			if err := email(id, report); err != nil {
				c.Logger().Errorf("couldn't send email: %v", err)
			}
			c.Logger().Debugf("note %s was reported", id)
		}
		return c.NoContent(http.StatusNoContent)
	})

	s := &http.Server{
		Addr:         ":3000",
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
	}
	e.Logger.Fatal(e.StartServer(s))
}

func fraudelent(n *Note) bool {
	stripped := rexpLink.ReplaceAllString(n.Text, "")
	l1 := len(n.Text)
	l2 := len(stripped)
	return n.Views > 100 &&
		int(math.Ceil(100*float64(l1-l2)/float64(l1))) > fraudThreshold
}
