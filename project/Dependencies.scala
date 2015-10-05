import sbt._

object Dependencies {
  def dependencies() = {
    Seq(
      "org.spire-math" %% "jawn-ast" % "0.8.3",
      "com.propensive" %% "rapture-json" % "2.0.0-M1",
      "com.propensive" %% "rapture-json-jawn" % "2.0.0-M1",
      "org.seleniumhq.selenium" % "selenium-java" % "2.47.2",
      "me.tongfei" % "progressbar_2.11" % "0.3.2",
      "org.scalatest" % "scalatest_2.11" % "2.2.5",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
    )
  }
}

