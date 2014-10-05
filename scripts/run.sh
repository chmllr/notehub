#!/bin/bash

export DEVMODE=1

if ! pgrep "redis-server" > /dev/null; then
    redis-server &
fi

lein ring server-headless 8080
