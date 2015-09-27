package grinder

import java.io.File

object Boot extends App{
  lazy val UserDir = {
    val UserDirProperty = System.getProperty("user.dir")
    if (UserDirProperty == null) "" else UserDirProperty
  }
  
  if (args.length < 1) {
    println("Please provide a command: prepare / compare")
    System.exit(1)
  }
  
  private val command = args(0)
  command match {
    case "prepare" => prepare()
    case "compare" => compare()
  }

  def prepare() {
    val parser = new CssReftestXmlParser
    val xmlFile = new File(s"$UserDir/nightly-unstable/xhtml1/reftest-toc.xht")

    println(xmlFile.getAbsolutePath)

    parser.parse(xmlFile)
  }

  def compare() {
    new CssTest().run()
    println("Run finished!")
  }
}
