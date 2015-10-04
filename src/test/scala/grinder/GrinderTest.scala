package grinder

import java.io.File

import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}

class GrinderTest extends FunSpec with Matchers with BeforeAndAfterAll{

  val cssTest = new Grinder(Seq("firefox"))
  
  describe("Image Comparison") {

    it("should return true if the image are same") {
      val testImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/abs-pos-non-replaced-vlr-005.xht.png")
      val refImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/abs-pos-non-replaced-vlr-006.xht.png")

      cssTest.isScreenShotSame(testImg,refImg) should be(true)
    }

    it("should return false if the image are different") {
      val testImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/abs-pos-non-replaced-vlr-005.xht.png")
      val refImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/abs-pos-non-replaced-vlr-013.xht.png")

      cssTest.isScreenShotSame(testImg,refImg) should be(false)
    }

    it("should return true the RGB values difference is less than threshold value") {
      val testImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/threshold_value_5_testhref.png")
      val refImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/threshold_value_5_refhref.png")

      cssTest.isScreenShotSame(testImg,refImg) should be(true)
    }

    it("should return false the RGB values difference is greater than threshold value") {
      val testImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/threshold_value_96_testhref.png")
      val refImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/threshold_value_96_refhref.png")

      cssTest.isScreenShotSame(testImg,refImg) should be(false)
    }

  }
  
  override def afterAll() ={
    super.afterAll()
    cssTest.driver.close()
  }

}
