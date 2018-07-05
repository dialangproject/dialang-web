#!/bin/sh

cd ..
#gradle --offline -x test build
gradle clean test build
sudo -S cp build/libs/dialang-web.war /srv/dialang-tomcat/webapps/ROOT.war
