#!/bin/sh

mvn -o clean compile package
sudo -S cp target/dialangweb-1.0-SNAPSHOT.war /usr/local/dialang-tomcat/webapps/dialang.war

