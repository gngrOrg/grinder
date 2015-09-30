import sbt._

object Dependencies {
  def dependencies() = {
    Seq(
      "org.seleniumhq.selenium" % "selenium-java" % "2.47.2",
      "me.tongfei" % "progressbar_2.11" % "0.3.1",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
    )
  }
}

