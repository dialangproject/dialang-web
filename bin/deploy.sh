#!/bin/sh

cd ..
mvn -o -Pdev clean install
sudo -S cp target/dialangweb-1.0-SNAPSHOT.war /usr/local/dialang-tomcat/webapps/ROOT.war

