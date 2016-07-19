package grinder

import java.awt.image.BufferedImage
import java.io.File

import javax.imageio.ImageIO

object GrinderUtil {
  private val FAILURE_THRESHOLD = 25
  private val COMPARISION_THRESHOLD = 10

  def isScreenShotSame(testFile: File, refImageFile: File, failureScale: Double = 1d): Boolean = {
    val scaledFailureThreshold = FAILURE_THRESHOLD * failureScale
    val referenceImage = ImageIO.read(refImageFile)

    if (!(testFile.exists() && refImageFile.exists())) {
      false
    } else if (isFileContentSame(testFile, refImageFile)) {
      true
    } else {
      val testImage = ImageIO.read(testFile)
      val referenceImage = ImageIO.read(refImageFile)

      if (hasEqualDimensions(testImage, referenceImage)) {
        var failures = 0
        var h = 0
        val width = testImage.getWidth
        val height = testImage.getHeight
        while (h < height && failures < scaledFailureThreshold) {
          var w = 0
          while (w < width && failures < scaledFailureThreshold) {
            val same = isPixelSimilar(testImage.getRGB(w, h), referenceImage.getRGB(w, h))
            if (!same) {
              failures += 1
            }
            w += 1
          }
          h += 1
        }
        failures < scaledFailureThreshold
      } else {
        false
      }
    }
  }

  private def isPixelSimilar(p1: Int, p2: Int): Boolean = {
    if (p1 == p2) {
      true
    } else {
      val r1 = (p1 >> 24) & 0xff;
      val g1 = (p1 >> 16) & 0xff;
      val b1 = (p1 >> 8) & 0xff;
      val a1 = (p1) & 0xff;

      val r2 = (p2 >> 24) & 0xff;
      val g2 = (p2 >> 16) & 0xff;
      val b2 = (p2 >> 8) & 0xff;
      val a2 = (p2) & 0xff;
      similar(r1, r2) && similar(g1, g2) && similar(b1, b2) && similar(a1, a2)
    }
  }

  private def similar(i1:Int, i2:Int): Boolean = {
    Math.abs(i1 - i2) < COMPARISION_THRESHOLD
  }

  private def hasEqualDimensions(testImage: BufferedImage, referenceImage: BufferedImage): Boolean = {
    testImage.getWidth == referenceImage.getWidth && testImage.getHeight == referenceImage.getHeight
  }

  import java.io.BufferedInputStream
  import java.io.FileInputStream

  private def isFileContentSame(file1: File, file2: File): Boolean = {
    if(file1.length != file2.length){
      false
    } else {

      val in1 =new BufferedInputStream(new FileInputStream(file1))
      val in2 =new BufferedInputStream(new FileInputStream(file2))

      var value1 = 0
      do {
         //since we're buffered read() isn't expensive
         value1 = in1.read()
         val value2 = in2.read()
         if(value1 !=value2) {
           return false
         }
      } while(value1 >=0)

      in1.close()
      in2.close()

      true
    }
  }

}
