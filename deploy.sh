#!/bin/sh

sbt package
cp target/scala-2.10/dialangweb_2.10-1.0.war /usr/local/dialang-tomcat/webapps/dialang.war

