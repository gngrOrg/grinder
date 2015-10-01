package grinder

import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import org.openqa.selenium.OutputType
import org.openqa.selenium.firefox.FirefoxDriver
import javax.imageio.ImageIO
import me.tongfei.progressbar.ProgressBar
import org.openqa.selenium.Dimension
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.openqa.selenium.WebDriverException

case class TestResult(id: String, pass: Boolean)

class Grinder(args: Seq[String]) {
  private val COMPARISION_THRESHOLD = 50
  private val resourceDir: String = s"${grinder.Boot.UserDir}/nightly-unstable"
  // private val referenceDirectory = s"file://$resourceDir/xhtml1"
  private val referenceDirectory = s"localhost:8000//nightly-unstable/xhtml1"
  private val imageDirectory: String = s"${grinder.Boot.UserDir}/data/screenshot"
  private val resultDirectory: String = s"${grinder.Boot.UserDir}/data/"

  val browserName = args(0)

  val driver = browserName match {
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
    Pause.init()

    val timer = new Timer()
    var passes = 0
    var fails = 0
    var results = Seq[TestResult]()
    val startDate = LocalDate.now()

    val parser = new TestXmlParser()
    val testCases = parser.parserTests
    val selectedTests = testCases // .drop(2000).take(10)

    try {
      // driver.manage().window().maximize()
      driver.manage().window().setSize(new Dimension(800, 800))

      driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS)

      val pb = new ProgressBar("Test", selectedTests.length)
      pb.start()
      timer.start()
      selectedTests.foreach { test =>
        val testHref  = new File(s"$imageDirectory/${enc(test.testHref)}.png")
        val refHref = new File(s"$imageDirectory/${enc(test.referenceHref)}.png")
        navAndSnap(test.testHref)
        navAndSnap(test.referenceHref)
        val same = isScreenShotSame(testHref,refHref)
        results +:= TestResult(test.testHref, same)
        if (same) {
          passes += 1
        } else {
          fails += 1
        }
        pb.step()
        pb.setExtraMessage("Fails: " + fails)

        if (Pause.isPauseRequested) {
          timer.stop()
          println(s"\n${Console.BOLD}Paused. Type `C` or `c` to continue, anything else to quit.${Console.RESET}")
          val response = io.StdIn.readLine()
          if (response.matches("[cC]")) {
            timer.start()
            Pause.init()
            println("Continuing")
          } else {
            printStats()
            throw new QuitRequestedException
          }
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

  def isScreenShotSame(testFile:File, refImageFile:File): Boolean = {
    val referenceImage = ImageIO.read(refImageFile)

    if (!(testFile.exists() && refImageFile.exists())) {
      false
    } else {
      val testImage = ImageIO.read(testFile)
      val referenceImage = ImageIO.read(refImageFile)

      val isExists = if (hasEqualDimensions(testImage, referenceImage)) {
        scalaxy.streams.optimize {
          var failures = 0
          for (
            w <- 0 until testImage.getWidth;
            h <- 0 until testImage.getHeight
          ) {
            val same = testImage.getRGB(w, h) == referenceImage.getRGB(w, h)
            if (!same) {
              failures += 1
            }
          }
          failures < COMPARISION_THRESHOLD
        }
        val count = comparisons.toList.count(_ == false)
        if(count > 0) println(s"************ $count "+testFile.getAbsolutePath+"\n"+refImageFile.getAbsolutePath)
        count < COMPARISION_THRESHOLD
      } else {
        false
      }
      isExists
    }
  }

  private def hasEqualDimensions(testImage: BufferedImage, referenceImage: BufferedImage): Boolean = {
    testImage.getWidth == referenceImage.getWidth && testImage.getHeight == referenceImage.getHeight
  }

  /*
  override def afterEach() = {
    FileUtils.cleanDirectory(new File(imageDirectory))
  }
  */
}

class Timer {
  private var consumedTime = 0L
  private var startTime = 0L
  private var running = false

  def start() {
    synchronized {
      running = true
      startTime = System.currentTimeMillis()
    }
  }

  def stop() {
    synchronized {
      consumedTime += System.currentTimeMillis() - startTime
      running = false
    }
  }

  def getConsumedTime:Long = {
    synchronized {
      if (running) {
        throw new IllegalStateException("Time being evaluated while running")
      }
      consumedTime
    }
  }
}