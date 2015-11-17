package grinder

import java.io.File
import java.io.FileOutputStream
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

case class TestResult(id: String, pass: Boolean)

class Grinder(args: Seq[String], options: Map[String, String]) {
  private val resourceDir: String = s"${grinder.Boot.UserDir}/nightly-unstable"
  private val referenceDirectory = s"localhost:8000//nightly-unstable/xhtml1"
  private val imageDirectory: String = s"${grinder.Boot.UserDir}/data/screenshot"
  private val resultDirectory: String = s"${grinder.Boot.UserDir}/data/"

  private val baseLineResultsOpt =
    options.get("baseLine").map{p =>
      val basePath = Paths.get(p).toAbsolutePath().normalize()
      println("Using baseline: " + basePath)
      ResultParser.getResults(basePath)
    }

  private val imgUploadEnabled = options.isDefinedAt("uploadImg")
  private val quitOnRegression = options.isDefinedAt("quitOnRegress")
  private val hrefFilterOpt = options.get("hrefFilter")
  private val failureScale = options.get("failureScale").map(java.lang.Double.parseDouble(_)).getOrElse(1d)

  val browserName = args(0)

  lazy val driver = browserName match {
    case "gngr" => {
      if (args.length > 1) {
        new GngrDriver(args(1))
      } else {
        throw new InvalidConfigurationException("Please specify the key")
      }
    }
    case "firefox" => new FirefoxDriver()
  }

  private def navigateToPage(link: String) {
    driver.navigate.to(s"$referenceDirectory/$link")
  }

  def run() {
    val imageDirectoryPath = FileSystems.getDefault().getPath(imageDirectory)
    java.nio.file.Files.createDirectories(imageDirectoryPath)

    Pause.init()

    val timer = new Timer()
    var passes = 0
    var fails = 0
    var progressions = 0
    var results = Seq[TestResult]()
    val startDate = LocalDate.now()

    val parser = new TestXmlParser()
    val testCases = parser.parserTests
    val selectedTests = hrefFilterOpt match {
      case Some(hrefFilter) =>
        testCases.filter(_.testHref contains hrefFilter)
      case None =>
        testCases
      }

    try {
      // driver.manage().window().maximize()
      driver.manage().window().setSize(new Dimension(800, 800))

      driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS)

      val pb = new ProgressBar("Test", selectedTests.length)
      pb.start()
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
          if (blResult.pass != "true" && same) {
            progressions += 1
          }
        }

        pb.step()
        pb.setExtraMessage("Fails: " + fails + " Progressions: " + progressions)

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

      println("Fails : " + fails)
      println("Passes: " + passes)

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
        results.map(r => json"""{
              "id": ${r.id},
              "pass": ${r.pass}
            }""")
      }
        }
      }"""
      val fw = new java.io.FileWriter(resultDirectory + "/results.json")
      try {
        fw.write(Json.format(json))
      } finally {
        fw.close()
      }
    }

    def isStopRequired(result: TestResult, baseLineResultOpt: Option[ResultParser.TestResult]): Boolean = {
      !result.pass && ({
        val regression = baseLineResultOpt.map(b => b.pass == "true").getOrElse(false)
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
    val bytes = driver.getScreenshotAs(OutputType.BYTES)
    val fileName = imageDirectory + "/" + name + ".png"
    val fos = new FileOutputStream(fileName)
    try {
      fos.write(bytes)
    } finally {
      fos.close()
    }
  }
}