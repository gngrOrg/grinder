
name := "grinder"

version := "1.4.2"

scalaVersion := "2.12.21"

// resolvers += Resolver.sonatypeRepo("releases"),

libraryDependencies ++= Dependencies.dependencies()

scalacOptions ++= Seq("-optimise")
