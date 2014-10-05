#!/bin/bash

unset DEVMODE

if ! pgrep "redis-server" > /dev/null; then
    redis-server &
fi

lein uberjar
java -jar target/NoteHub-2.0.0-standalone.jar
