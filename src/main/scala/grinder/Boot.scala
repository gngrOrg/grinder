package grinder

import java.io.File
import java.nio.file._
import org.openqa.selenium.OutputType

object Boot extends App {
  lazy val UserDir = {
    val UserDirProperty = System.getProperty("user.dir")
    if (UserDirProperty == null) "" else UserDirProperty
  }

  if (args.length < 1) {
    println("Please provide a command:")
    println("  prepare [inputDir]")
    println("  compare <browserName> [key] [--profileDir=<path>] [--reportDir=<path>]")
    System.exit(1)
  }

  private val command = args(0)
  try {
    command match {
      case "prepare" =>
        val (argsRem, optionsMap) = separateArgsOptions(args.tail)
        val inputDir = optionsMap.get("inputDir")
          .orElse(optionsMap.get("input-dir"))
          .map(_.trim)
          .filter(_.nonEmpty)
          .orElse(argsRem.headOption)
          .getOrElse("nightly-unstable")
        prepare(inputDir)
      case "compare" => compare()
      case "checkChange" => checkChange()
      case "updateBase" => UpdateBaseLine.update(args.tail)
      case "checkBase" => CheckBaseLine.check(args.tail)
      case "benchmark" => benchmark()
    }
  } catch {
    case te: TestException => println(te.summary)
  }

  def prepare(inputDir: String = "nightly-unstable") {
    val parser = new CssReftestXmlParser
    val baseDir = new File(inputDir)
    val resolvedDir = if (baseDir.isAbsolute) baseDir else new File(UserDir, inputDir)
    val xmlFile = if (new File(resolvedDir, "reftest-toc.xht").exists()) {
      new File(resolvedDir, "reftest-toc.xht")
    } else {
      new File(resolvedDir, "xhtml1/reftest-toc.xht")
    }

    println(xmlFile.getAbsolutePath)

    val testCaseXml = parser.parse(xmlFile)

    val dataDirStr = grinder.Boot.UserDir + "/data/"
    val dataDirPath = FileSystems.getDefault().getPath(dataDirStr)
    java.nio.file.Files.createDirectories(dataDirPath)

    XmlUtils.saveXmlFile("test-cases",testCaseXml.toString(), dataDirStr)

  }

  def compare() {
    val (argsRem, optionsMap) = separateArgsOptions(args.tail)
    if (argsRem.nonEmpty) {
      val cssTest = new Grinder(argsRem, optionsMap)
      cssTest.run()
      println("Run finished!")
    } else {
      println("Please specify the browser: [gngr / firefox]")
    }
  }

  def checkChange() {
    val (argsRem, optionsMap) = separateArgsOptions(args.tail)
    if (argsRem.nonEmpty) {
      val changeTestChecker = new ChangeChecker(argsRem, optionsMap)
      changeTestChecker.run()
      println("Run finished!")
    } else {
      println("Please specify the browser: [gngr / firefox]")
    }
  }

  def benchmark() {
    val (argsRem, optionsMap) = separateArgsOptions(args.tail)
    if (argsRem.nonEmpty) {
      val keyOpt = optionsMap.get("key").map(_.trim).filter(_.nonEmpty)
      val profileDirOpt = GngrDriver.extractProfileDir(optionsMap)
      keyOpt match {
        case Some(key) =>
          val driver = new GngrDriver(key, profileDirOpt)
          val repeatCount = 6
          var totalElapsedTime = 0L
          for (i <- 0 until repeatCount) {
          val startTime = System.currentTimeMillis()
            driver.navigate.to(argsRem(0))
            val bytes = driver.getScreenshotAs(OutputType.BYTES)
            val endTime = System.currentTimeMillis()
            totalElapsedTime += (endTime - startTime)
            println(s"$i : Elapsed time: ${endTime - startTime}, Total: $totalElapsedTime")
          }
          println(s"Time taken: ${totalElapsedTime/repeatCount.toDouble}")
          driver.close()
        case None =>
          println("auth key required. specify with --key=xxx")
      }
    } else {
      println("Please specify the url")
    }
  }

  private[grinder] def separateArgsOptions(args: Array[String]): (Seq[String], Map[String, String]) = {
    val (optionsStr, argsRem) = args.partition(_.startsWith("--"))
    val options = optionsStr.flatMap { os =>
      val raw = os.stripPrefix("--")
      val eqIdx = raw.indexOf('=')
      if (eqIdx != -1) {
        val key = raw.substring(0, eqIdx).trim
        val value = raw.substring(eqIdx + 1).trim
        if (key.nonEmpty) Some(key -> unquote(value)) else None
      } else {
        val key = raw.trim
        if (key.nonEmpty) Some(key -> "") else None
      }
    }
    val optionsMap = options.toMap
    (argsRem.toSeq, optionsMap)
  }

  private def unquote(s: String): String = {
    if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
      if (s.length >= 2) s.substring(1, s.length - 1) else s
    } else s
  }

}
