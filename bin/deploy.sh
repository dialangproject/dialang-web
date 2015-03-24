#!/bin/sh

cd ..
gradle --offline -x test build
sudo -S cp build/libs/dialang-web.war /usr/local/dialang-tomcat/webapps/ROOT.war

