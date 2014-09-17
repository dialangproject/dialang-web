#!/bin/sh

cd ..
gradle -Pprofile=live clean build
scp build/libs/dialang-web.war dialangweb:~

