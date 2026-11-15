package grinder

import better.files._
import java.io.{File => JFile}
import java.io.FileOutputStream
import java.nio.file.Files
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
import java.nio.file.Path

case class ChangeTestResult(id: String, change: Boolean)

class ChangeChecker(args: Seq[String], options: Map[String, String]) {
  private val resourceDir = File(s"${grinder.Boot.UserDir}/changeTests/")
  private val urlBase = s"localhost:8000//changeTests"

  val reportBaseDir: java.nio.file.Path = ReportConfig.resolveReportBaseDir(options)
  lazy val reportRunPaths: ReportPaths = ReportConfig.createRunPaths(reportBaseDir)

  private def imageDirectory: File = File(reportRunPaths.screenshotsDir)
  private def resultDirectory: File = File(reportRunPaths.runDir)

  private val nameFilterOpt = options.get("nameFilter").map(_.trim).filter(_.nonEmpty)

  private val quitOnChange = options.isDefinedAt("quitOnChange")
  private val failureScale = options.get("failureScale")
    .map(_.trim)
    .filter(_.nonEmpty)
    .flatMap(s => scala.util.Try(java.lang.Double.parseDouble(s)).toOption)
    .getOrElse(1d)
  val profileDirOpt = GngrDriver.extractProfileDir(options)

  val browserName = args(0)

  lazy val driver = browserName match {
    case "gngr" => {
      val keyOpt = args.lift(1).orElse(options.get("key")).map(_.trim).filter(_.nonEmpty)
      keyOpt match {
        case Some(key) => new GngrDriver(key, profileDirOpt)
        case None => throw new InvalidConfigurationException("Please specify the key")
      }
    }
    case "firefox" => new FirefoxDriver()
  }

  private def navigateToPage(link: String): Unit = {
    driver.navigate.to(s"$urlBase/$link")
  }

  def run(): Unit = {
    val runPaths = reportRunPaths
    println(s"Reports and screenshots will be written to: ${runPaths.runDir}")

    imageDirectory.createDirectories()

    Pause.init()

    val timer = new Timer()
    var sames = 0
    var changes = 0
    var results = Seq[ChangeTestResult]()
    val startDate = LocalDate.now()

    val testCases =
      (resourceDir / "tests")
      .collectChildren(c => (!c.isDirectory) && (c.name.endsWith("html") || c.name.endsWith("xht")))(using File.VisitOptions.follow)
      .toList

    val selectedTests = nameFilterOpt match {
      case Some(nameFilter) =>
        testCases.filter(_.name.contains(nameFilter))
      case None =>
        testCases
      }

    try {
      driver.manage().window().setSize(new Dimension(800, 800))

      driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS)

      val pb = ProgressBar.builder()
        .setTaskName("Test")
        .setInitialMax(selectedTests.length)
        .build()
      var pbStopped = false
      def stopProgressBar(): Unit = {
        if (!pbStopped) {
          pbStopped = true
          pb.close()
        }
      }
      try {
        timer.start()
        selectedTests.foreach { testFile =>
          val test = (resourceDir / "tests").relativize(testFile).toString
          val encodedFileName = enc(test) + ".png"
          val testImage = imageDirectory / encodedFileName
          val refImage = resourceDir / "screenshot" / encodedFileName
          val testBytesOpt = navAndSnap(test, testImage)
          val same = refImage.exists && GrinderUtil.isScreenShotSame(testImage.toJava, refImage.toJava, failureScale, testBytesOpt, None)
          val result = ChangeTestResult(test, !same)
          results +:= result
          if (same) {
            sames += 1
          } else {
            changes += 1
          }
          pb.step()
          pb.setExtraMessage("Changes: " + changes)

          if (Pause.isPauseRequested) {
            pb.pause()
            timer.stop()
            printStats()
            println(s"\n${Console.BOLD}Paused, stats written. Type `C` or `c` to continue, anything else to quit.${Console.RESET}")
            val response = io.StdIn.readLine()
            if (response != null && response.matches("[cC]")) {
              pb.resume()
              timer.start()
              Pause.init()
              println("Continuing")
            } else {
              stopProgressBar()
              printStats()
              throw new QuitRequestedException
            }
          } else if (isStopRequired(result)) {
            timer.stop()

            stopProgressBar()
            printStats()
            throw new QuitRequestedException
          }
        }
        timer.stop()
      } finally {
        stopProgressBar()
      }
    } finally {
      driver.quit()
    }

    printStats()

    def printStats() = {
      import com.google.gson.{GsonBuilder, JsonArray, JsonObject}

      println("Changes : " + changes)
      println("Sames: " + sames)

      val metaObj = new JsonObject()
      metaObj.addProperty("browser", browserName)
      metaObj.addProperty("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
      metaObj.addProperty("timeTaken", java.lang.Long.valueOf(timer.getConsumedTime))

      val resultsArr = new JsonArray()
      results.foreach { r =>
        val itemObj = new JsonObject()
        itemObj.addProperty("id", r.id)
        itemObj.addProperty("change", java.lang.Boolean.valueOf(r.change))
        resultsArr.add(itemObj)
      }

      val changeTestsObj = new JsonObject()
      changeTestsObj.addProperty("totalCount", Integer.valueOf(testCases.length))
      changeTestsObj.addProperty("selectedCount", Integer.valueOf(selectedTests.length))
      changeTestsObj.add("results", resultsArr)

      val json = new JsonObject()
      json.add("meta", metaObj)
      json.add("change-tests", changeTestsObj)

      val jsonFormatted = new GsonBuilder().setPrettyPrinting().create().toJson(json)
      val fw = new java.io.FileWriter((resultDirectory / "results.json").path.toString)
      try {
        fw.write(jsonFormatted)
      } finally {
        fw.close()
      }

      try {
        val legacyDataDir = File(s"${grinder.Boot.UserDir}/data/changeTests")
        if (legacyDataDir.exists) {
          (legacyDataDir / "results.json").write(jsonFormatted)
        }
      } catch {
        case _: Exception =>
      }
    }

    def isStopRequired(result: ChangeTestResult): Boolean = {
      result.change && quitOnChange 
    }
  }

  private var visited: Set[String] = Set()

  private def navAndSnap(path: String, targetFile: File): Option[Array[Byte]] = {
    if (!visited.contains(path)) {
      visited += path
      try {
        navigateToPage("tests/" + path)
        Some(takeScreenShot(targetFile))
      } catch {
        case wde: WebDriverException =>
          println(s"\nError for $path : ${wde.getMessage}")
          None
      }
    } else {
      None
    }
  }

  private def enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

  private def takeScreenShot(file: File): Array[Byte] = {
    val bytes = driver.getScreenshotAs(OutputType.BYTES)
    val fos = file.newOutputStream
    try {
      fos.write(bytes)
    } finally {
      fos.close()
    }
    bytes
  }
}
