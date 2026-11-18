package grinder

import java.io.File
import java.nio.file._
import org.openqa.selenium.OutputType

object Boot {
  lazy val UserDir = {
    val UserDirProperty = System.getProperty("user.dir")
    if (UserDirProperty == null) "" else UserDirProperty
  }

  /** Prints top-level CLI usage and all available commands. */
  def printHelp(): Unit = {
    println("Please provide a command:")
    println("  prepare [inputDir] [--inputDir=<path>|--input-dir=<path>]")
    println("  compare <browserName> [key] [--key=<key>] [--profileDir=<path>|--profile=<name>] [--reportDir=<path>|--report-dir=<path>] [--baseLine=<path>] [--hrefFilter=<substring>] [--failureScale=<number>] [--quitOnRegress]")
    println("  checkChange <browserName> [key] [--key=<key>] [--profileDir=<path>|--profile=<name>] [--reportDir=<path>|--report-dir=<path>] [--nameFilter=<substring>] [--failureScale=<number>] [--quitOnChange]")
    println("  checkBase <newDir> <baseDir>")
    println("  updateBase <newDir> <baseDir> [--yes|--all]")
    println("  benchmark <url> --key=<key> [--profileDir=<path>|--profile=<name>]")
  }

  /** Prints usage help for the `prepare` command. */
  def printPrepareHelp(): Unit = {
    println("Usage: prepare [inputDir] [--inputDir=<path>|--input-dir=<path>]")
    println("  inputDir                  Input directory containing reftest-toc.xht or xhtml1/reftest-toc.xht (default: nightly-unstable)")
    println("  --inputDir, --input-dir   Alternative way to specify inputDir")
  }

  /** Prints usage help for the `compare` command. */
  def printCompareHelp(): Unit = {
    println("Usage: compare <browserName> [key] [options]")
    println("  <browserName>             Browser to drive: gngr or firefox")
    println("  [key]                     Auth key for gngr (or pass --key=<key>)")
    println("  --key=<key>               Auth key for gngr")
    println("  --profileDir=<path>       Path to gngr profile directory")
    println("  --profile=<name>          Profile name under ~/.gngr/ (or path)")
    println("  --reportDir=<path>        Report output directory (default: ./grinder-report/)")
    println("  --report-dir=<path>       Alias for --reportDir")
    println("  --baseLine=<path>         Baseline run directory to compare against")
    println("  --hrefFilter=<substring>  Only run tests whose href contains <substring>")
    println("  --failureScale=<number>   Scale factor (default: 1.0) for pixel-difference failure threshold")
    println("  --quitOnRegress           Stop immediately when a regression against --baseLine is found")
  }

  /** Prints usage help for the `checkChange` command. */
  def printCheckChangeHelp(): Unit = {
    println("Usage: checkChange <browserName> [key] [options]")
    println("  <browserName>             Browser to drive: gngr or firefox")
    println("  [key]                     Auth key for gngr (or pass --key=<key>)")
    println("  --key=<key>               Auth key for gngr")
    println("  --profileDir=<path>       Path to gngr profile directory")
    println("  --profile=<name>          Profile name under ~/.gngr/ (or path)")
    println("  --reportDir=<path>        Report output directory (default: ./grinder-report/)")
    println("  --report-dir=<path>       Alias for --reportDir")
    println("  --nameFilter=<substring>  Only run tests whose filename contains <substring>")
    println("  --failureScale=<number>   Scale factor (default: 1.0) for pixel-difference failure threshold")
    println("  --quitOnChange            Stop immediately when a visual change is detected")
  }

  /** Prints usage help for the `benchmark` command. */
  def printBenchmarkHelp(): Unit = {
    println("Usage: benchmark <url> --key=<key> [--profileDir=<path>|--profile=<name>]")
    println("  <url>                     URL to navigate and screenshot 6 times")
    println("  --key=<key>               Auth key for gngr (required)")
    println("  --profileDir=<path>       Path to gngr profile directory")
    println("  --profile=<name>          Profile name under ~/.gngr/ (or path)")
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      printHelp()
      System.exit(1)
    }

    val command = args(0)
    try {
      command match {
        case "prepare" =>
          val (argsRem, optionsMap) = separateArgsOptions(args.tail)
          if (optionsMap.contains("help")) {
            printPrepareHelp()
          } else {
            val inputDir = optionsMap.get("inputDir")
              .orElse(optionsMap.get("input-dir"))
              .map(_.trim)
              .filter(_.nonEmpty)
              .orElse(argsRem.headOption)
              .getOrElse("nightly-unstable")
            prepare(inputDir)
          }
        case "compare" => compare(args.tail)
        case "checkChange" => checkChange(args.tail)
        case "updateBase" => UpdateBaseLine.update(args.tail)
        case "checkBase" => CheckBaseLine.check(args.tail)
        case "benchmark" => benchmark(args.tail)
        case "help" | "--help" | "-h" => printHelp()
        case other =>
          println(s"Unknown command: $other")
          printHelp()
          System.exit(1)
      }
    } catch {
      case te: TestException => println(te.summary)
    }
  }

  def prepare(inputDir: String = "nightly-unstable"): Unit = {
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

    XmlUtils.saveXmlFile("test-cases", testCaseXml, dataDirStr)

  }

  def compare(args: Array[String]): Unit = {
    val (argsRem, optionsMap) = separateArgsOptions(args)
    if (argsRem.isEmpty || optionsMap.contains("help")) {
      printCompareHelp()
    } else {
      val cssTest = new Grinder(argsRem, optionsMap)
      cssTest.run()
      println("Run finished!")
    }
  }

  def checkChange(args: Array[String]): Unit = {
    val (argsRem, optionsMap) = separateArgsOptions(args)
    if (argsRem.isEmpty || optionsMap.contains("help")) {
      printCheckChangeHelp()
    } else {
      val changeTestChecker = new ChangeChecker(argsRem, optionsMap)
      changeTestChecker.run()
      println("Run finished!")
    }
  }

  def benchmark(args: Array[String]): Unit = {
    val (argsRem, optionsMap) = separateArgsOptions(args)
    if (argsRem.isEmpty || optionsMap.contains("help")) {
      printBenchmarkHelp()
    } else {
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
          printBenchmarkHelp()
      }
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
