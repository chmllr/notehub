# This one is necessary to start the app in :dev mode, because
# I changed the default mode to the production, because AFAIK
# it's not possible to parameterize the app start on Heroku.
run:
	lein run dev

server:
	lein cljsbuild auto &
	redis-server &
	java vimclojure.nailgun.NGServer
