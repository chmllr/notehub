server:
	lein cljsbuild auto &
	redis-server &
	java vimclojure.nailgun.NGServer
