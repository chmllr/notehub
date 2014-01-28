# starts the app in :dev mode
run:
	@DEVMODE=1 lein ring server

server:
	redis-server &
