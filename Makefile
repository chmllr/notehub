run:
	SKIP_CAPTCHA=1 go run *.go

db:
	echo 'CREATE TABLE "notes" (`id` VARCHAR(6) UNIQUE PRIMARY KEY, `text` TEXT, `published` TIMESTAMP DEFAULT CURRENT_TIMESTAMP, `edited` TIMESTAMP DEFAULT NULL, `password` VARCHAR(16), `views` INTEGER DEFAULT 0);' | sqlite3 database.sqlite
