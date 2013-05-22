#!/bin/sh

cd ..
mvn -o -Plive clean install
scp target/dialangweb-1.0-SNAPSHOT.war dialangweb:~

