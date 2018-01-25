package main

import (
	"database/sql"
	"sync"
	"time"

	"github.com/labstack/echo"
)

const statsSavingInterval = 1 * time.Minute

var stats = &sync.Map{}

func flushStatsLoop(logger echo.Logger, db *sql.DB) {
	for {
		c, err := flush(db)
		if err != nil {
			logger.Errorf("couldn't flush stats: %v", err)
		}

		if c > 0 {
			logger.Infof("successfully persisted %d values", c)
		}
		time.Sleep(statsSavingInterval)
	}
}

func flush(db *sql.DB) (int, error) {
	c := 0
	tx, err := db.Begin()
	if err != nil {
		return c, err
	}
	stats.Range(func(id, views interface{}) bool {
		stmt, _ := tx.Prepare("update notes set views = ? where id = ?")
		_, err := stmt.Exec(views, id)
		if err == nil {
			c++
		}
		stmt.Close()
		defer stats.Delete(id)
		return true
	})
	return c, tx.Commit()
}

func incViews(n *Note, db *sql.DB) {
	views := n.Views
	if viewsCached, found := stats.Load(n.ID); found {
		if val, ok := viewsCached.(int); ok {
			views = val
		}
	}
	stats.Store(n.ID, views+1)
	if TEST_MODE {
		flush(db)
	}
}
