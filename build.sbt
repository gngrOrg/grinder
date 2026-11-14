
name := "grinder"

version := "1.4.3-dev"

scalaVersion := "2.13.18"

// resolvers += Resolver.sonatypeRepo("releases"),

libraryDependencies ++= Dependencies.dependencies()

scalacOptions ++= Seq("-opt:l:inline")
