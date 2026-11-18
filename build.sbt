
name := "grinder"

version := "1.4.4"

scalaVersion := "3.9.0"

// resolvers += Resolver.sonatypeRepo("releases"),

libraryDependencies ++= Dependencies.dependencies()

scalacOptions ++= Seq("-opt", "-opt-inline:grinder.**", "-Wopt:all", "-Wconf:msg=Scala3InlineInfo:s", "-deprecation", "-feature")
