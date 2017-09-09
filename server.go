package main

import (
	"io/ioutil"
	"net/http"

	"github.com/labstack/echo"
	"github.com/russross/blackfriday"
)

func main() {
	e := echo.New()

	e.Static("/", "assets/public")

	e.GET("/Demo.md", func(c echo.Context) error { return c.String(http.StatusOK, mdPage(c, "Demo")) })
	e.GET("/TOS.md", func(c echo.Context) error { return c.String(http.StatusOK, mdPage(c, "TOS")) })

	e.GET("/", func(c echo.Context) error {
		return c.String(http.StatusOK, "Hello, World!")
	})
	e.Logger.Fatal(e.Start(":3000"))
}

func mdPage(c echo.Context, name string) string {
	path := "assets/markdown/" + name + ".md"
	mdContent, err := ioutil.ReadFile(path)
	if err != nil {
		c.Logger().Errorf("couldn't open markdown page %q: %v", path, err)
		return ""
	}
	return string(blackfriday.Run(mdContent))
}
