
name := "grinder"

version := "1.4.3-dev"

scalaVersion := "3.9.0"

// resolvers += Resolver.sonatypeRepo("releases"),

libraryDependencies ++= Dependencies.dependencies()

scalacOptions ++= Seq("-opt", "-opt-inline:grinder.**,utils.**,parser.**", "-Wopt:all", "-Wconf:msg=Scala3InlineInfo:s", "-deprecation", "-feature")
