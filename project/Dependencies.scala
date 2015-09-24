import sbt._

object Dependencies {
  def dependencies() = {
    Seq(
      "org.seleniumhq.selenium" % "selenium-java" % "2.45.0",
      "org.scalatest" % "scalatest_2.10" % "2.2.4" % "test->*",
      "org.pegdown" % "pegdown" % "1.4.2" % "test"
    )
  }
}