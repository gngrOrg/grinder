import sbt._

object Dependencies {
  def dependencies() = {
    Seq(
      "org.seleniumhq.selenium" % "selenium-java" % "2.45.0",
      "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test->*",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
      // "org.pegdown" % "pegdown" % "1.4.2" % "test"
    )
  }
}
