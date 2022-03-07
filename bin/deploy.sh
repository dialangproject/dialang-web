#!/bin/sh

#export JAVA_HOME=`/usr/libexec/java_home -v 1.8`

cd ..
#gradle --offline -x test build
gradle clean test build
sudo -S cp build/libs/dialang-web.war /Users/fisha/srv/dialang-tomcat/webapps/ROOT.war
