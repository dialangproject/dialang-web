#!/bin/sh

cd ..
gradle -Pprofile=live clean build
scp build/libs/dialang-web.war dialang-web1:~
scp build/libs/dialang-web.war dialang-web2:~

