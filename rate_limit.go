package main

import (
	"sync"
	"time"

	"github.com/labstack/echo"
)

const (
	rateLimit         = 20 // times per rateLimitInterval
	rateLimitInterval = 1 * time.Hour
)

var accesses = &sync.Map{}

type access struct {
	count     int
	timestamp time.Time
}

func legitAccess(c echo.Context) bool {
	ip := c.Request().RemoteAddr
	aRaw, found := accesses.Load(ip)
	var a *access
	if found {
		a, _ = aRaw.(*access)
	} else {
		a = &access{}
	}
	a.count++
	a.timestamp = time.Now()
	accesses.Store(ip, a)
	return a.count < rateLimit
}

func cleanAccessRegistry(logger echo.Logger) {
	for {
		time.Sleep(rateLimitInterval)
		t, e := 0, 0
		accesses.Range(func(ip, aRaw interface{}) bool {
			t++
			a, _ := aRaw.(*access)
			if a.timestamp.Add(rateLimitInterval).Before(time.Now()) {
				accesses.Delete(ip)
				e++
			}
			return true
		})
		if e > 0 {
			logger.Infof("cleaned up %d/%d outdated accesses", e, t)
		}
	}
}
