import sbt._

object Dependencies {
  def dependencies() = {
    Seq(
      // "com.nativelibs4java" %% "scalaxy-streams" % "0.3.4" % "provided",
      "org.spire-math" %% "jawn-ast" % "0.10.1",
      "com.propensive" %% "rapture-json" % "2.0.0-M7",
      "com.propensive" %% "rapture-json-jawn" % "2.0.0-M7",
      "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" intransitive(),
      "org.seleniumhq.selenium" % "selenium-firefox-driver" % "2.53.1",
      "me.tongfei" % "progressbar" % "0.4.1",
      "org.scalatest" % "scalatest_2.11" % "3.0.0",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "com.github.pathikrit" %% "better-files" % "2.16.0",

      // Added for imgur upload, could be removed later
      "org.apache.httpcomponents" % "httpmime" % "4.5.2"
    )
  }
}

