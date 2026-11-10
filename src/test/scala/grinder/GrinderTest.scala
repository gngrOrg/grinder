package grinder

import java.io.File

import org.scalatest.funspec._
import org.scalatest.matchers.should._

class GrinderTest extends AnyFunSpec with Matchers {

  describe("Image Comparison") {

    it("should return true if the image are same") {
      val testImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/abs-pos-non-replaced-vlr-005.xht.png")
      val refImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/abs-pos-non-replaced-vlr-006.xht.png")

      GrinderUtil.isScreenShotSame(testImg, refImg) should be(true)
    }

    it("should return false if the images are different") {
      {
        val testImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/abs-pos-non-replaced-vlr-005.xht.png")
        val refImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/abs-pos-non-replaced-vlr-013.xht.png")

        GrinderUtil.isScreenShotSame(testImg, refImg) should be(false)
      }

      {
        val testImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/one.png")
        val refImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/two.png")

        GrinderUtil.isScreenShotSame(testImg, refImg) should be(false)
      }
    }

    it("should return true the RGB values difference is less than threshold value") {
      val testImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/threshold_value_5_testhref.png")
      val refImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/threshold_value_5_refhref.png")

      GrinderUtil.isScreenShotSame(testImg, refImg) should be(true)
    }

    it("should return false the RGB values difference is greater than threshold value") {
      val testImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/threshold_value_96_testhref.png")
      val refImg = new File(s"${grinder.Boot.UserDir}/src/test/resources/screenshot/threshold_value_96_refhref.png")

      GrinderUtil.isScreenShotSame(testImg, refImg) should be(false)
    }

  }

  describe("Prepare Command") {
    it("should accept an input directory parameter and parse test cases") {
      val tempDir = java.nio.file.Files.createTempDirectory("grinder-test-prepare").toFile
      try {
        val xhtmlDir = new File(tempDir, "xhtml1")
        xhtmlDir.mkdirs()
        val sampleToc =
          <html xmlns="http://www.w3.org/1999/xhtml">
            <body>
              <table>
                <tbody>
                  <tr>
                    <td title="Test 1"><a href="test1.xht">Test 1</a></td>
                    <td><a href="test1-ref.xht">=</a></td>
                    <td></td>
                  </tr>
                </tbody>
              </table>
            </body>
          </html>
        scala.xml.XML.save(new File(xhtmlDir, "reftest-toc.xht").getAbsolutePath, sampleToc, enc = "utf-8")

        Boot.prepare(tempDir.getAbsolutePath)

        val outputFile = new File(s"${Boot.UserDir}/data/test-cases.xml")
        outputFile.exists() should be(true)
        val loaded = scala.xml.XML.loadFile(outputFile)
        (loaded \\ "test").length should be(1)
        (loaded \\ "test" \ "test-title").text should be("Test 1")
      } finally {
        def deleteRecursively(f: File): Unit = {
          if (f.isDirectory) f.listFiles().foreach(deleteRecursively)
          f.delete()
        }
        deleteRecursively(tempDir)
      }
    }
  }

  describe("Gngr Profile Directory") {
    it("should resolve absolute profile directory") {
      val absDir = new File("/tmp/custom-gngr-profile")
      val resolved = GngrDriver.resolveProfileDir(absDir.getAbsolutePath)
      resolved should be(absDir)
    }

    it("should resolve tilde path relative to user home") {
      val userHome = System.getProperty("user.home")
      val resolved = GngrDriver.resolveProfileDir("~/custom-profile")
      resolved should be(new File(userHome, "custom-profile"))
    }

    it("should extract profileDir option") {
      val opts1 = Map("profileDir" -> "/tmp/p1")
      GngrDriver.extractProfileDir(opts1) should be(Some(new File("/tmp/p1")))

      val opts2 = Map("profile" -> "my-profile")
      val userHome = System.getProperty("user.home")
      val expectedUnderGngr = new File(new File(userHome, ".gngr"), "my-profile")
      GngrDriver.extractProfileDir(opts2) should be(Some(expectedUnderGngr))
    }

    it("should return None when profileDir or profile is empty or whitespace") {
      GngrDriver.extractProfileDir(Map("profileDir" -> "")) should be(None)
      GngrDriver.extractProfileDir(Map("profileDir" -> "   ")) should be(None)
      GngrDriver.extractProfileDir(Map("profile" -> "")) should be(None)
      GngrDriver.extractProfileDir(Map("profile" -> "  ")) should be(None)
    }

    it("should populate profileDirOpt in Grinder") {
      val grinder = new Grinder(Seq("gngr", "test-key"), Map("profileDir" -> "/tmp/custom-profile"))
      grinder.profileDirOpt should be(Some(new File("/tmp/custom-profile")))

      val grinderEmpty = new Grinder(Seq("gngr", "test-key"), Map("profileDir" -> ""))
      grinderEmpty.profileDirOpt should be(None)
    }

    it("should fail with clear path when port.dat is missing in custom profile directory") {
      val tempDir = java.nio.file.Files.createTempDirectory("grinder-custom-profile").toFile
      try {
        val ex = intercept[InvalidConfigurationException] {
          new GngrDriver("test-key", tempDir)
        }
        ex.getMessage should include(tempDir.getAbsolutePath)
        ex.getMessage should include("port.dat")
      } finally {
        tempDir.delete()
      }
    }
  }

  describe("CLI Option Parsing") {
    it("should parse normal options and remaining arguments") {
      val (argsRem, options) = Boot.separateArgsOptions(Array("gngr", "key123", "--profileDir=/tmp/profile"))
      argsRem should be(Seq("gngr", "key123"))
      options should be(Map("profileDir" -> "/tmp/profile"))
    }

    it("should handle ill-specified options without throwing exceptions") {
      val (argsRem1, opts1) = Boot.separateArgsOptions(Array("--profileDir="))
      argsRem1 should be(empty)
      opts1 should be(Map("profileDir" -> ""))

      val (argsRem2, opts2) = Boot.separateArgsOptions(Array("--anotherOption="))
      argsRem2 should be(empty)
      opts2 should be(Map("anotherOption" -> ""))

      val (argsRem3, opts3) = Boot.separateArgsOptions(Array("--uploadImg", "--quitOnRegress"))
      argsRem3 should be(empty)
      opts3 should be(Map("uploadImg" -> "", "quitOnRegress" -> ""))

      val (argsRem4, opts4) = Boot.separateArgsOptions(Array("--", "--=", "--=val", "--key=val=extra"))
      argsRem4 should be(empty)
      opts4 should be(Map("key" -> "val=extra"))
    }

    it("should handle unquoting option values") {
      val (_, opts1) = Boot.separateArgsOptions(Array("--profileDir=\"/path/with quotes\""))
      opts1 should be(Map("profileDir" -> "/path/with quotes"))

      val (_, opts2) = Boot.separateArgsOptions(Array("--key=''"))
      opts2 should be(Map("key" -> ""))
    }
  }

}
