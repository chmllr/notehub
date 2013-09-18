# starts the app in :dev mode
run:
	lein run dev

server:
	lein cljsbuild auto &
	redis-server &
