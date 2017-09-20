package main

import (
	"errors"
	"fmt"
	"html/template"
	"io/ioutil"
	"net/http"
	"regexp"
	"strings"

	"github.com/golang-commonmark/markdown"
	"github.com/labstack/echo"
)

var (
	statuses = map[int]string{
		400: "Bad request",
		401: "Unauthorized",
		404: "Not found",
		412: "Precondition failed",
		429: "Too many requests",
		503: "Service unavailable",
	}

	rexpNewLine        = regexp.MustCompile("[\n\r]")
	rexpNonAlphaNum    = regexp.MustCompile("[`~!@#$%^&*_|+=?;:'\",.<>{}\\/]")
	rexpNoScriptIframe = regexp.MustCompile("<.*?(iframe|script).*?>")
	rexpLink           = regexp.MustCompile("(ht|f)tp://[^\\s]+")

	errorUnathorised = errors.New("id or password is wrong")
	errorBadRequest  = errors.New("password is empty")
)

func responsePage(code int, details ...string) *Note {
	text := statuses[code]
	body := text
	if len(details) > 0 {
		body += ": " + strings.Join(details, ";")
	}
	n := &Note{
		Title: text,
		Text:  fmt.Sprintf("# %d %s", code, body),
	}
	n.prepare()
	return n
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

var mdRenderer = markdown.New(markdown.HTML(true))

func mdTmplHTML(content []byte) template.HTML {
	return template.HTML(mdRenderer.RenderToString(content))
}

func md2html(c echo.Context, name string) (*Note, int) {
	path := "assets/markdown/" + name + ".md"
	mdContent, err := ioutil.ReadFile(path)
	if err != nil {
		c.Logger().Errorf("couldn't open markdown page %q: %v", path, err)
		code := http.StatusServiceUnavailable
		return responsePage(code), code
	}
	c.Logger().Debugf("rendering markdown page %q", name)
	return &Note{Title: name, Content: mdTmplHTML(mdContent)}, http.StatusOK
}
