package main

import (
	"bytes"
	"encoding/json"
	"html/template"
	"io"
	"io/ioutil"
	"math"
	"net/http"
	"net/url"
	"os"
	"time"

	"database/sql"

	_ "github.com/mattn/go-sqlite3"

	"github.com/labstack/echo"
	"github.com/labstack/gommon/log"
)

const fraudThreshold = 7

var TEST_MODE = false

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

	TEST_MODE = os.Getenv("TEST_MODE") != ""

	var ads []byte
	adsFName := os.Getenv("ADS")
	if adsFName != "" {
		var err error
		ads, err = ioutil.ReadFile(adsFName)
		if err != nil {
			e.Logger.Errorf("couldn't read file %s: %v", adsFName, err)
		}
	}

	go flushStatsLoop(e.Logger, db)

	e.Renderer = &Template{templates: template.Must(template.ParseGlob("assets/templates/*.html"))}

	e.File("/favicon.ico", "assets/public/favicon.ico")
	e.File("/robots.txt", "assets/public/robots.txt")
	e.File("/style.css", "assets/public/style.css")
	e.File("/new.js", "assets/public/new.js")
	e.File("/note.js", "assets/public/note.js")
	e.File("/index.html", "assets/public/index.html")
	e.File("/", "assets/public/index.html")

	e.GET("/TOS.md", func(c echo.Context) error {
		n, code := md2html(c, "TOS")
		if code != http.StatusOK {
			c.String(code, statuses[code])
		}
		return c.Render(code, "Page", n)
	})

	e.GET("/:id", func(c echo.Context) error {
		id := c.Param("id")
		n, code := load(c, db)
		if code != http.StatusOK {
			c.Logger().Errorf("/%s failed (code: %d)", code)
			return c.String(code, statuses[code])
		}
		defer incViews(n, db)
		fraud := fraudelent(n)
		if fraud {
			n.Ads = mdTmplHTML(ads)
		}
		c.Logger().Debugf("/%s delivered (fraud: %t)", id, fraud)
		return c.Render(code, "Note", n)
	})

	e.GET("/:id/export", func(c echo.Context) error {
		id := c.Param("id")
		n, code := load(c, db)
		var content string
		if code == http.StatusOK {
			defer incViews(n, db)
			if fraudelent(n) {
				code = http.StatusForbidden
				content = statuses[code]
				c.Logger().Warnf("/%s/export failed (code: %d)", id, code)
			} else {
				content = n.Text
				c.Logger().Debugf("/%s/export delivered", id)
			}
		}
		return c.String(code, content)
	})

	e.GET("/:id/stats", func(c echo.Context) error {
		id := c.Param("id")
		n, code := load(c, db)
		if code != http.StatusOK {
			c.Logger().Errorf("/%s/stats failed (code: %d)", code)
			return c.String(code, statuses[code])
		}
		n.prepare()
		buf := bytes.NewBuffer([]byte{})
		e.Renderer.Render(buf, "Stats", n, c)
		n.Content = template.HTML(buf.String())
		c.Logger().Debugf("/%s/stats delivered", id)
		return c.Render(code, "Note", n)
	})

	e.GET("/:id/edit", func(c echo.Context) error {
		id := c.Param("id")
		n, code := load(c, db)
		if code != http.StatusOK {
			c.Logger().Errorf("/%s/edit failed (code: %d)", code)
			return c.String(code, statuses[code])
		}
		c.Logger().Debugf("/%s/edit delivered", id)
		return c.Render(code, "Form", n)
	})

	e.POST("/:id/report", func(c echo.Context) error {
		report := c.FormValue("report")
		if report != "" {
			id := c.Param("id")
			c.Logger().Infof("note %s was reported: %s", id, report)
			if err := email(id, report); err != nil {
				c.Logger().Errorf("couldn't send email: %v", err)
			}
		}
		return c.NoContent(http.StatusNoContent)
	})

	e.GET("/new", func(c echo.Context) error {
		c.Logger().Debug("/new opened")
		return c.Render(http.StatusOK, "Form", nil)
	})

	type postResp struct {
		Success bool
		Payload string
	}

	e.POST("/", func(c echo.Context) error {
		c.Logger().Debug("POST /")
		if !TEST_MODE && !checkRecaptcha(c, c.FormValue("token")) {
			code := http.StatusForbidden
			return c.JSON(code, postResp{false, statuses[code] + ": robot check failed"})
		}
		if c.FormValue("tos") != "on" {
			code := http.StatusPreconditionFailed
			c.Logger().Errorf("POST / error: %d", code)
			return c.JSON(code, postResp{false, statuses[code]})
		}
		id := c.FormValue("id")
		text := c.FormValue("text")
		l := len(text)
		if (id == "" || id != "" && l != 0) && (10 > l || l > 50000) {
			code := http.StatusBadRequest
			c.Logger().Errorf("POST / error: %d", code)
			return c.JSON(code, postResp{false, statuses[code] + ": note length not accepted"})
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
			c.Logger().Errorf("POST / error: %d", code)
			return c.JSON(code, postResp{false, statuses[code] + ": " + err.Error()})
		}
		if id == "" {
			c.Logger().Infof("note %s created", n.ID)
			return c.JSON(http.StatusCreated, postResp{true, n.ID})
		} else if text == "" {
			c.Logger().Infof("note %s deleted", n.ID)
		} else {
			c.Logger().Infof("note %s updated", n.ID)
		}
		return c.JSON(http.StatusOK, postResp{true, n.ID})
	})

	s := &http.Server{
		Addr:         ":3000",
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
	}
	e.Logger.Fatal(e.StartServer(s))
}

func fraudelent(n *Note) bool {
	res := rexpLink.FindAllString(n.Text, -1)
	if len(res) < 3 {
		return false
	}
	stripped := rexpLink.ReplaceAllString(n.Text, "")
	l1 := len(n.Text)
	l2 := len(stripped)
	return n.Views > 100 &&
		int(math.Ceil(100*float64(l1-l2)/float64(l1))) > fraudThreshold
}

func checkRecaptcha(c echo.Context, captchaResp string) bool {
	resp, err := http.PostForm("https://www.google.com/recaptcha/api/siteverify", url.Values{
		"secret":   []string{os.Getenv("RECAPTCHA_SECRET")},
		"response": []string{captchaResp},
		"remoteip": []string{c.Request().RemoteAddr},
	})
	if err != nil {
		c.Logger().Errorf("captcha response verification failed: %v", err)
		return false
	}
	defer resp.Body.Close()
	respJson := &struct {
		Success    bool     `json:"success"`
		ErrorCodes []string `json:"error-codes"`
	}{}
	s, err := ioutil.ReadAll(resp.Body)
	err = json.Unmarshal(s, respJson)
	if err != nil {
		c.Logger().Errorf("captcha response parse recaptcha response: %v", err)
		return false
	}
	if !respJson.Success {
		c.Logger().Warnf("captcha validation failed: %v", respJson.ErrorCodes)
	}
	return respJson.Success

}
