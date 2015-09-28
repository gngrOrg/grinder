package grinder

import java.io.File

object Boot extends App {
  lazy val UserDir = {
    val UserDirProperty = System.getProperty("user.dir")
    if (UserDirProperty == null) "" else UserDirProperty
  }

  if (args.length < 1) {
    println("Please provide a command:")
    println("  prepare")
    println("  compare <browserName>")
    System.exit(1)
  }

  private val command = args(0)
  try {
    command match {
      case "prepare" => prepare()
      case "compare" => compare()
    }
  } catch {
    case te: TestException => println(te.summary)
  }

  def prepare() {
    val parser = new CssReftestXmlParser
    val xmlFile = new File(s"$UserDir/nightly-unstable/xhtml1/reftest-toc.xht")

    println(xmlFile.getAbsolutePath)

    parser.parse(xmlFile)
  }

  def compare() {
    if (args.length > 1) {
      val cssTest = new CssTest(args.tail)
      cssTest.run()
      println("Run finished!")
    } else {
      println("Please specify the browser: [gngr / firefox]")
    }
  }
}
