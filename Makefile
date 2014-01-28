# starts the app in :dev mode
run:
	@DEVMODE=1 lein ring server 8080

server:
	redis-server &
