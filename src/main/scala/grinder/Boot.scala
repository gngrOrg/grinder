package grinder

import java.io.File
import java.nio.file._

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
      case "updateBase" => UpdateBaseLine.update(args.tail)
      case "checkBase" => CheckBaseLine.check(args.tail)
    }
  } catch {
    case te: TestException => println(te.summary)
  }

  def prepare() {
    val parser = new CssReftestXmlParser
    val xmlFile = new File(s"$UserDir/nightly-unstable/xhtml1/reftest-toc.xht")

    println(xmlFile.getAbsolutePath)

    val testCaseXml = parser.parse(xmlFile)

    val dataDirStr = grinder.Boot.UserDir + "/data/"
    val dataDirPath = FileSystems.getDefault().getPath(dataDirStr)
    java.nio.file.Files.createDirectories(dataDirPath)

    XmlUtils.saveXmlFile("test-cases",testCaseXml.toString(), dataDirStr)

  }

  def compare() {
    if (args.length > 1) {
      val (argsRem, optionsMap) = separateArgsOptions(args.tail)
      val cssTest = new Grinder(argsRem, optionsMap)
      cssTest.run()
      println("Run finished!")
    } else {
      println("Please specify the browser: [gngr / firefox]")
    }
  }

  private def separateArgsOptions(args: Array[String]) = {
    val (optionsStr, argsRem) = args.partition(_.startsWith("--"))
    val options = optionsStr.map{os =>
      val firstTwoFields = os.substring(2).split('=').filter(_.length > 0).take(2)
      (firstTwoFields(0), firstTwoFields(1))
    }
    val optionsMap = options.toMap
    (argsRem, optionsMap)
  }

}
