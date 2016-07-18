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
  private val imageDirectory = File(s"${grinder.Boot.UserDir}/data/changeTests/screenshot")
  private val resultDirectory = File(s"${grinder.Boot.UserDir}/data/changeTests/")

  private val nameFilterOpt = options.get("nameFilter")

  private val quitOnChange = options.isDefinedAt("quitOnChange")
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
    driver.navigate.to(s"$urlBase/$link")
  }

  def run() {
    imageDirectory.createDirectories

    Pause.init()

    val timer = new Timer()
    var sames = 0
    var changes = 0
    var results = Seq[ChangeTestResult]()
    val startDate = LocalDate.now()

    val testCases = (resourceDir / "tests").collectChildren(c => (!c.isDirectory) && c.name.endsWith("html"))(File.VisitOptions.follow).toList
    val selectedTests = nameFilterOpt match {
      case Some(nameFilter) =>
        testCases.filter(_.name contains nameFilter)
      case None =>
        testCases
      }

    try {
      driver.manage().window().setSize(new Dimension(800, 800))

      driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS)

      val pb = new ProgressBar("Test", selectedTests.length)
      pb.start()
      timer.start()
      selectedTests.foreach { testFile =>
        val test = (resourceDir / "tests").relativize(testFile).toString
        val testImage = imageDirectory/(enc(test)+".png")
        val refImage = resourceDir/"screenshot"/(enc(test)+".png")
        navAndSnap(test)
        val same = refImage.exists && GrinderUtil.isScreenShotSame(testImage.toJava, refImage.toJava, failureScale)
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
        } else if (isStopRequired(result)) {
          timer.stop()

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

      println("Changes : " + changes)
      println("Sames: " + sames)

      val json = json"""{
        "meta" : {
          "browser"   : $browserName,
          "startDate" : ${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)},
          "timeTaken" : ${timer.getConsumedTime}
        },
        "change-tests" : {
          "totalCount" : ${testCases.length},
          "selectedCount" : ${selectedTests.length},
          "results": ${
            results.map(r => json"""{
              "id": ${r.id},
              "change": ${r.change}
            }""")
          }
        }
      }"""
      val fw = new java.io.FileWriter((resultDirectory / "results.json").path.toString)
      try {
        fw.write(Json.format(json))
      } finally {
        fw.close()
      }
    }

    def isStopRequired(result: ChangeTestResult): Boolean = {
      result.change && quitOnChange 
    }
  }

  private var visited: Set[String] = Set()

  private def navAndSnap(path: String) {
    if (!visited.contains(path)) {
      visited += path
      try {
        navigateToPage("tests/" + path)
        takeScreenShot(enc(path))
      } catch {
        case wde: WebDriverException => println(s"\nError for $path : ${wde.getMessage}")
      }
    }
  }

  private def enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

  private def takeScreenShot(name: String) {
    val bytes = driver.getScreenshotAs(OutputType.BYTES)
    val file= imageDirectory / (name + ".png")
    val fos = file.newOutputStream
    try {
      fos.write(bytes)
    } finally {
      fos.close()
    }
  }
}
