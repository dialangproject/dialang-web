#!/bin/sh

cd ..
gradle -Pprofile=live build
scp build/libs/dialang-web.war dialangweb:~

