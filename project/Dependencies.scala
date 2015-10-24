import sbt._

object Dependencies {
  def dependencies() = {
    Seq(
      // "com.nativelibs4java" %% "scalaxy-streams" % "0.3.4" % "provided",
      "org.spire-math" %% "jawn-ast" % "0.8.3",
      "com.propensive" %% "rapture-json" % "2.0.0-M1",
      "com.propensive" %% "rapture-json-jawn" % "2.0.0-M1",
      "org.seleniumhq.selenium" % "selenium-java" % "2.48.2" intransitive(),
      "org.seleniumhq.selenium" % "selenium-firefox-driver" % "2.48.2",
      "me.tongfei" % "progressbar_2.11" % "0.3.2",
      "org.scalatest" % "scalatest_2.11" % "2.2.5",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
      "com.codeborne" % "phantomjsdriver" % "1.2.1",
      // Added for imgur upload, could be removed later
      "org.apache.httpcomponents" % "httpmime" % "4.5.1"
    )
  }
}

