import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfterEach, Matchers}
import parser.TestXmlParser
import utils.BrowserDriver

class CssTest extends BrowserDriver with Matchers with BeforeAndAfterEach {

  val COMPARISION_THRESHOLD = 50
  private val resourceDir: String = s"${System.getProperty("user.dir")}/src/test/resources"
  val referenceDirectory = s"file://$resourceDir/xhtml1"
  val imageDirectory: String = s"$resourceDir/screenshot"

  protected def navigateToPage(link: String) = go.to(s"$referenceDirectory/$link")(driver)

  describe("Css Test") {
    val parser = new TestXmlParser()
    val testCases = parser.parserTests
    testCases.foreach { test => {
      it("should pass for test: " + test.testHref) {
        navigateToPage(test.testHref)
        takeScreenShot(test.testHref)
        navigateToPage(test.referenceHref)
        takeScreenShot(test.referenceHref)
        val comparison = isScreenShotSame(test.testHref.replace("reference/", ""), test.referenceHref.replace("reference/", ""))
        comparison should be(true)
      }
    }
    }
  }


  def takeScreenShot(name: String) = {
    setCaptureDir(imageDirectory)
    captureTo(name.replace("reference/", ""))
  }

  def isScreenShotSame(test: String, ref: String): Boolean = {
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

  override def afterEach() = {
    FileUtils.cleanDirectory(new File(imageDirectory))
  }
}
