
name := "grinder"

version := "1.4.0"

scalaVersion := "2.12.8"

// resolvers += Resolver.sonatypeRepo("releases"),

libraryDependencies ++= Dependencies.dependencies()

scalacOptions ++= Seq("-optimise")
