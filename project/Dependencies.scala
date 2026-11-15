import sbt._

object Dependencies {
  def dependencies() = {
    Seq(
      // "com.nativelibs4java" %% "scalaxy-streams" % "0.3.4" % "provided",
      "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" intransitive(),
      "org.seleniumhq.selenium" % "selenium-firefox-driver" % "2.53.1",

      "me.tongfei" % "progressbar" % "0.10.2",

      "org.scalatest" %% "scalatest" % "3.2.20",
      "org.scala-lang.modules" %% "scala-xml" % "2.5.0",
      "com.github.pathikrit" %% "better-files" % "3.9.2"
    )
  }
}

