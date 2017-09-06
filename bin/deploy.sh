#!/bin/sh

cd ..
gradle --offline -x test build
sudo -S cp build/libs/dialang-web.war /srv/dialang-tomcat1/webapps/ROOT.war
sudo -S cp build/libs/dialang-web.war /srv/dialang-tomcat2/webapps/ROOT.war
