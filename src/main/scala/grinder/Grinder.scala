package grinder

import java.io.File
import java.util.concurrent.TimeUnit
import org.openqa.selenium.OutputType
import org.openqa.selenium.firefox.FirefoxDriver
import me.tongfei.progressbar.ProgressBar
import org.openqa.selenium.Dimension
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.openqa.selenium.WebDriverException
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.nio.file.Files

case class TestResult(id: String, pass: Boolean)

class Grinder(args: Seq[String], options: Map[String, String]) {
  private val resourceDir: String = s"${grinder.Boot.UserDir}/nightly-unstable"
  private val referenceDirectory = s"localhost:8000//nightly-unstable/xhtml1"

  val reportBaseDir: java.nio.file.Path = ReportConfig.resolveReportBaseDir(options)
  lazy val reportRunPaths: ReportPaths = ReportConfig.createRunPaths(reportBaseDir)

  private def imageDirectory: String = reportRunPaths.screenshotsDir.toString
  private def resultDirectory: String = reportRunPaths.runDir.toString

  private val baseLineResultsOpt =
    options.get("baseLine").map{p =>
      val basePath = Paths.get(p).toAbsolutePath().normalize()
      println("Using baseline: " + basePath)
      ResultParser.getResults(basePath)
    }

  if (!baseLineResultsOpt.isDefined) {
    println(Console.RED + Console.BOLD + "\nBase line not defined!!\n" + Console.RESET)
  }

  private val imgUploadEnabled = options.isDefinedAt("uploadImg")
  private val quitOnRegression = options.isDefinedAt("quitOnRegress")
  private val hrefFilterOpt = options.get("hrefFilter").map(_.trim).filter(_.nonEmpty)
  private val failureScale = options.get("failureScale")
    .map(_.trim)
    .filter(_.nonEmpty)
    .flatMap(s => scala.util.Try(java.lang.Double.parseDouble(s)).toOption)
    .getOrElse(1d)
  val profileDirOpt = GngrDriver.extractProfileDir(options)

  val browserName = args(0)

  lazy val driver = browserName match {
    case "gngr" => {
      val keyOpt = (if (args.length > 1) Some(args(1)) else options.get("key")).map(_.trim).filter(_.nonEmpty)
      keyOpt match {
        case Some(key) => new GngrDriver(key, profileDirOpt)
        case None => throw new InvalidConfigurationException("Please specify the key")
      }
    }
    case "firefox" => new FirefoxDriver()
  }

  private def navigateToPage(link: String) {
    driver.navigate.to(s"$referenceDirectory/$link")
  }

  def run() {
    val runPaths = reportRunPaths
    println(s"Reports and screenshots will be written to: ${runPaths.runDir}")

    val imageDirectoryPath = runPaths.screenshotsDir
    Files.createDirectories(imageDirectoryPath)

    Pause.init()

    val timer = new Timer()
    var passes = 0
    var fails = 0
    var progressions = 0
    var regressions = 0
    var results = Seq[TestResult]()
    val startDate = LocalDate.now()

    val parser = new TestXmlParser()
    val testCases = parser.parserTests
    val filteredTests = hrefFilterOpt match {
      case Some(hrefFilter) =>
        testCases.filter(_.testHref contains hrefFilter)
      case None =>
        testCases
      }
    val selectedTests = Jumbler.jumbledUp(filteredTests, 0)

    try {
      // driver.manage().window().maximize()
      driver.manage().window().setSize(new Dimension(800, 800))

      driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS)

      val pb = new ProgressBar("Test", selectedTests.length, 250)
      // pb.start()
      timer.start()
      selectedTests.foreach { test =>
        val testHref = new File(s"$imageDirectory/${enc(test.testHref)}.png")
        val refHref = new File(s"$imageDirectory/${enc(test.referenceHref)}.png")
        navAndSnap(test.testHref)
        navAndSnap(test.referenceHref)
        val same = GrinderUtil.isScreenShotSame(testHref, refHref, failureScale)
        val result = TestResult(test.testHref, same)
        results +:= result
        if (same) {
          passes += 1
        } else {
          fails += 1
        }

        val baseLineResultOpt = baseLineResultsOpt match {
          case Some(baseLineResults) =>
            baseLineResults.find(_.id == result.id)
          case _ => None
        }

        baseLineResultOpt.foreach {blResult =>
          if (same) {
            if(!blResult.pass) { progressions += 1 }
          } else {
            if(blResult.pass) { regressions += 1 }
          }
        }

        pb.step()
        pb.setExtraMessage("Fails: " + fails + (if (baseLineResultsOpt.isDefined) {" Progs: " + progressions } else ""))

        if (Pause.isPauseRequested) {
          timer.stop()
          printStats()
          println(s"\n${Console.BOLD}Paused, stats written. Type `C` or `c` to continue, anything else to quit.${Console.RESET}")
          val response = io.StdIn.readLine()
          if (response.matches("[cC]")) {
            timer.start()
            Pause.init()
            println("Continuing")
          } else {
            printStats()
            throw new QuitRequestedException
          }
        } else if (isStopRequired(result, baseLineResultOpt)) {
          timer.stop()

          if (imgUploadEnabled) {
            val testPath = testHref.toPath()
            ImgUpload.uploadImgur(testPath)
            val refPath = refHref.toPath()
            ImgUpload.uploadImgur(refPath)
          }

          printStats()
          throw new QuitRequestedException
        }
      }
      timer.stop()
      pb.stop()
    } finally {
      driver.quit()
    }

    printStats()

    def printStats() = {
      import rapture.json._
      import jsonBackends.jawn._
      import formatters.humanReadable.jsonFormatterImplicit

      println("Fails       : " + fails)
      println("Passes      : " + passes)
      println("Progressions: " + (if (baseLineResultsOpt.isDefined) ("" + progressions) else "N/A"))
      println("Regressions : " + (if (baseLineResultsOpt.isDefined) ("" + regressions) else "N/A"))

      val json = json"""{
        "meta" : {
          "browser"   : $browserName,
          "startDate" : ${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)},
          "timeTaken" : ${timer.getConsumedTime}
        },
        "css21-reftests" : {
          "totalCount" : ${testCases.length},
          "selectedCount" : ${selectedTests.length},
          "results": ${
            results.sortBy(_.id).reverse.map(r => json"""{
              "id": ${r.id},
              "pass": ${r.pass}
            }""")
          }
        }
      }"""
      val jsonFormatted = Json.format(json)
      val fw = new java.io.FileWriter(resultDirectory + "/results.json")
      try {
        fw.write(jsonFormatted)
      } finally {
        fw.close()
      }

      try {
        val legacyDataDir = Paths.get(grinder.Boot.UserDir, "data")
        if (Files.exists(legacyDataDir)) {
          Files.write(legacyDataDir.resolve("results.json"), jsonFormatted.getBytes("UTF-8"))
        }
      } catch {
        case _: Exception =>
      }
    }

    def isStopRequired(result: TestResult, baseLineResultOpt: Option[ResultParser.TestResult]): Boolean = {
      !result.pass && ({
        val regression = baseLineResultOpt.map(b => b.pass).getOrElse(false)
        if (regression) {
          println("\nFound regression: " + result.id)
        }
        quitOnRegression && regression
      })
    }
  }

  private var visited: Set[String] = Set()

  private def navAndSnap(path: String) {
    if (!visited.contains(path)) {
      visited += path
      try {
        navigateToPage(path)
        takeScreenShot(enc(path))
      } catch {
        case wde: WebDriverException => println(s"\nError for $path : ${wde.getMessage}")
      }
    }
  }

  private def enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

  private def takeScreenShot(name: String) {
    val bytes:Array[Byte] = driver.getScreenshotAs(OutputType.BYTES)
    val fileName = imageDirectory + "/" + name + ".png"
    FileUtils.writeFile(fileName, bytes)
  }
}
