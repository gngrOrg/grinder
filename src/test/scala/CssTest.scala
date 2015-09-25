import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

import org.scalatest.{BeforeAndAfterEach, Matchers}
import parser.TestXmlParser
import utils.BrowserDriver

class CssTest extends BrowserDriver with Matchers with BeforeAndAfterEach {

  val COMPARISION_THRESHOLD = 50
  private val resourceDir: String = s"${boot.Boot.UserDir}/nightly-unstable"
  private val referenceDirectory = s"file://$resourceDir/xhtml1"
  private val imageDirectory: String = s"${boot.Boot.UserDir}/data/screenshot"

  protected def navigateToPage(link: String) = go.to(s"$referenceDirectory/$link")(driver)

  describe("Css Test") {
    val parser = new TestXmlParser()
    val testCases = parser.parserTests
    testCases.foreach { test =>
      it("should pass for test: " + test.testHref) {
        navAndSnap(test.testHref)
        navAndSnap(test.referenceHref)
        isScreenShotSame(enc(test.testHref), enc(test.referenceHref)) should be(true)
      }
    }
  }

  private var visited:Set[String] = Set()

  private def navAndSnap(path:String) {
    if (!visited.contains(path)) {
      visited += path
      navigateToPage(path)
      takeScreenShot(enc(path))
    }
  }


  private def enc(s:String) = java.net.URLEncoder.encode(s, "UTF-8")

  private def takeScreenShot(name: String) = {
    setCaptureDir(imageDirectory)
    captureTo(name)
  }

  private def isScreenShotSame(test: String, ref: String): Boolean = {
    val testFile: File = new File(s"$imageDirectory/$test.png")
    val testImage = ImageIO.read(testFile)
    val refImage: File = new File(s"$imageDirectory/$ref.png")
    val referenceImage = ImageIO.read(refImage)

    val isExists = if (isEqualDimensions(testImage, referenceImage)) {
      val comparisons = for (w <- 0 until testImage.getWidth;
                             h <- 0 until testImage.getHeight) yield {
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

  def isEqualDimensions(testImage: BufferedImage, referenceImage: BufferedImage): Boolean = {
    testImage.getWidth == referenceImage.getWidth && testImage.getWidth == referenceImage.getWidth
  }

  /*
  override def afterEach() = {
    FileUtils.cleanDirectory(new File(imageDirectory))
  }
  */
}
