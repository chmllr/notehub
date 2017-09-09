package main

import (
	"html/template"
	"io"
	"io/ioutil"
	"net/http"

	"github.com/labstack/echo"
	"github.com/russross/blackfriday"
)

type Template struct{ templates *template.Template }

func (t *Template) Render(w io.Writer, name string, data interface{}, c echo.Context) error {
	return t.templates.ExecuteTemplate(w, name, data)
}

func main() {
	e := echo.New()
	e.Renderer = &Template{templates: template.Must(template.ParseGlob("assets/templates/*.html"))}
	e.Static("/", "assets/public")
	e.GET("/TOS.md", func(c echo.Context) error { return c.Render(http.StatusOK, "Page", md2html(c, "TOS")) })
	e.Logger.Fatal(e.Start(":3000"))
}

type Note struct {
	ID, Title string
	Content   template.HTML
}

func md2html(c echo.Context, name string) *Note {
	path := "assets/markdown/" + name + ".md"
	mdContent, err := ioutil.ReadFile(path)
	if err != nil {
		c.Logger().Errorf("couldn't open markdown page %q: %v", path, err)
		return nil
	}
	return &Note{Content: template.HTML(string(blackfriday.Run(mdContent)))}
}
