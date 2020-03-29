
name := "grinder"

version := "1.4.1"

scalaVersion := "2.12.10"

// resolvers += Resolver.sonatypeRepo("releases"),

libraryDependencies ++= Dependencies.dependencies()

scalacOptions ++= Seq("-optimise")
