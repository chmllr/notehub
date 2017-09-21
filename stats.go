package main

import (
	"database/sql"
	"sync"
	"time"

	"github.com/labstack/echo"
)

const statsSavingInterval = 1 * time.Minute

var stats = &sync.Map{}

func persistStats(logger echo.Logger, db *sql.DB) {
	for {
		time.Sleep(statsSavingInterval)
		tx, err := db.Begin()
		if err != nil {
			logger.Error(err)
			return
		}
		c := 0
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
		tx.Commit()
		if c > 0 {
			logger.Infof("successfully persisted %d values", c)
		}
	}
}

func incViews(n *Note) {
	views := n.Views
	if val, ok := stats.Load(n.ID); ok {
		intVal, ok := val.(int)
		if ok {
			views = intVal
		}
	}
	defer stats.Store(n.ID, views+1)
}
