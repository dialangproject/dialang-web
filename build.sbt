scalaVersion := "2.10.0"

libraryDependencies += "javax.servlet" % "servlet-api" % "2.5" % "provided"

organization := "DIALANG"

name := "dialangweb"

version := "1.0"

retrieveManaged := true

seq(webSettings :_*)

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"
