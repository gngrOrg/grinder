import Dependencies._
import sbt.Keys._
import sbt._

object Build extends Build {

  val appName = "grinder"

  val main = Project(appName, file(".")).settings(

    // (testOptions in Test) +=Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports"),
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= dependencies(),
    scalacOptions ++= Seq("-optimise", "-Yclosure-elim", "-Yinline", "-YGenBCode")
  )
}

