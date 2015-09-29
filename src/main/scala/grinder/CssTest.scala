package grinder

import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import org.openqa.selenium.OutputType
import org.openqa.selenium.firefox.FirefoxDriver
import javax.imageio.ImageIO
import me.tongfei.progressbar.ProgressBar

class CssTest(args:Seq[String]) {
  private val COMPARISION_THRESHOLD = 50
  private val resourceDir: String = s"${grinder.Boot.UserDir}/nightly-unstable"
  // private val referenceDirectory = s"file://$resourceDir/xhtml1"
  private val referenceDirectory = s"localhost:8000//nightly-unstable/xhtml1"
  private val imageDirectory: String = s"${grinder.Boot.UserDir}/data/screenshot"

  val browserName = args(0)

  private val driver = browserName match {
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
    var passes = 0
    var fails = 0

    try {
      driver.manage().window().maximize()

      driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS)

      val parser = new TestXmlParser()
      val testCases = parser.parserTests
      val selectedTests = testCases.drop(2100).take(100)
      val pb = new ProgressBar("Test", selectedTests.length)
      pb.start()
      selectedTests.foreach { test =>
        navAndSnap(test.testHref)
        navAndSnap(test.referenceHref)
        val same = isScreenShotSame(enc(test.testHref), enc(test.referenceHref))
        if (same) {
          passes += 1
        } else {
          fails += 1
        }
        pb.step()
      }
      pb.stop()
    } finally {
      driver.quit()
    }
    }
    println("Fails : " + fails)
    println("Passes: " + passes)
  }

  private var visited: Set[String] = Set()

  private def navAndSnap(path: String) {
    if (!visited.contains(path)) {
      visited += path
      navigateToPage(path)
      takeScreenShot(enc(path))
    }
  }

  private def enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

  private def takeScreenShot(name: String) = {
    val bytes = driver.getScreenshotAs(OutputType.BYTES)
    val fileName = imageDirectory + "/" + name + ".png"
    val fos = new FileOutputStream(fileName)
    try {
      fos.write(bytes)
    } finally {
      fos.close()
    }
  }

  private def isScreenShotSame(test: String, ref: String): Boolean = {
    val testFile: File = new File(s"$imageDirectory/$test.png")
    val testImage = ImageIO.read(testFile)
    val refImage: File = new File(s"$imageDirectory/$ref.png")
    val referenceImage = ImageIO.read(refImage)

    val isExists = if (hasEqualDimensions(testImage, referenceImage)) {
      val comparisons = for (
        w <- 0 until testImage.getWidth;
        h <- 0 until testImage.getHeight
      ) yield {
        val result = testImage.getRGB(w, h) == referenceImage.getRGB(w, h)
        result
      }
      val count = comparisons.toList.count(_ == false)
      count < COMPARISION_THRESHOLD
    } else {
      false
    }
    isExists
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
