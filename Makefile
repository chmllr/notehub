# starts the app in :dev mode
run:
	@DEVMODE=1 lein run

server:
	redis-server &
