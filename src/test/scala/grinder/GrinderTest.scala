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

    it("should support in-memory screenshot comparison and cache/invalidate reference images") {
      import java.awt.image.BufferedImage
      import java.io.ByteArrayOutputStream
      import java.nio.file.Files
      import javax.imageio.ImageIO

      def makePngBytes(argb: Int): Array[Byte] = {
        val img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
        for (y <- 0 until 8; x <- 0 until 8) {
          img.setRGB(x, y, argb)
        }
        val baos = new ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        baos.toByteArray
      }

      val greenBytes = makePngBytes(0xff00ff00)
      val nearGreenBytes = makePngBytes(0xff00fa00)
      val redBytes = makePngBytes(0xffff0000)
      val nearRedBytes = makePngBytes(0xfffa0000)

      val tempDir = Files.createTempDirectory("grinder-img-cache-test").toFile
      try {
        GrinderUtil.clearRefCache()

        val missingTest = new File(tempDir, "missing-test.png")
        val missingRef = new File(tempDir, "missing-ref.png")
        GrinderUtil.isScreenShotSame(missingTest, missingRef, 1d, Some(greenBytes), Some(greenBytes)) should be(true)
        GrinderUtil.isScreenShotSame(missingTest, missingRef, 1d, Some(greenBytes), Some(nearGreenBytes)) should be(true)
        GrinderUtil.isScreenShotSame(missingTest, missingRef, 1d, Some(greenBytes), Some(redBytes)) should be(false)

        GrinderUtil.clearRefCache()
        val refFile = new File(tempDir, "shared-ref.png")
        Files.write(refFile.toPath, redBytes)

        GrinderUtil.isScreenShotSame(missingTest, refFile, 1d, Some(greenBytes), None) should be(false)
        GrinderUtil.isRefDecodedCached(refFile) should be(true)
        GrinderUtil.refCacheHits should be(0L)

        GrinderUtil.isScreenShotSame(missingTest, refFile, 1d, Some(redBytes), None) should be(true)
        GrinderUtil.refCacheHits should be(1L)

        GrinderUtil.isScreenShotSame(missingTest, refFile, 1d, Some(nearRedBytes), None) should be(true)
        GrinderUtil.refCacheHits should be(2L)

        val previousMod = refFile.lastModified()
        Files.write(refFile.toPath, greenBytes)
        refFile.setLastModified(previousMod + 2000L)

        GrinderUtil.isScreenShotSame(missingTest, refFile, 1d, Some(greenBytes), None) should be(true)
        GrinderUtil.refCacheHits should be(2L)

        GrinderUtil.isScreenShotSame(missingTest, refFile, 1d, Some(redBytes), None) should be(false)
        GrinderUtil.refCacheHits should be(3L)
      } finally {
        GrinderUtil.clearRefCache()
        better.files.File(tempDir.toPath).delete()
      }
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

    it("should skip unsupported flags before extracting other fields in CssReftestXmlParser") {
      val tempDir = java.nio.file.Files.createTempDirectory("grinder-test-parser").toFile
      try {
        val tocFile = new File(tempDir, "reftest-toc.xht")
        val sampleToc =
          <html xmlns="http://www.w3.org/1999/xhtml">
            <body>
              <table>
                <tbody>
                  <tr>
                    <td title="Supported No Flags"><a href="sup-none.xht">Supported No Flags</a></td>
                    <td><a href="sup-none-ref.xht">=</a></td>
                    <td></td>
                  </tr>
                </tbody>
                <tbody>
                  <tr>
                    <td title="Supported Flags"><a href="sup-flags.xht">Supported Flags</a></td>
                    <td><a href="sup-flags-ref.xht">!=</a></td>
                    <td><abbr title="Requires Ahem">Ahem</abbr><abbr title="Uses image">image</abbr></td>
                  </tr>
                </tbody>
                <tbody>
                  <tr>
                    <td title="Unsupported Interact"><span>no link</span></td>
                    <td><span>no ref</span></td>
                    <td><abbr title="Requires interaction">interact</abbr></td>
                  </tr>
                </tbody>
                <tbody>
                  <tr>
                    <td title="Unsupported Paged"><a href="unsup-paged.xht">Paged</a></td>
                    <td><a href="unsup-paged-ref.xht">=</a></td>
                    <td><abbr title="Paged media">paged</abbr></td>
                  </tr>
                </tbody>
                <tbody>
                  <tr>
                    <td title="Unsupported DOM"><a href="unsup-dom.xht">DOM</a></td>
                    <td><a href="unsup-dom-ref.xht">=</a></td>
                    <td><abbr title="Requires DOM">dom</abbr></td>
                  </tr>
                </tbody>
                <tbody>
                  <tr>
                    <td title="Unsupported Mixed"><a href="unsup-domjs.xht">DOM/JS</a></td>
                    <td><a href="unsup-domjs-ref.xht">=</a></td>
                    <td><abbr title="Requires Ahem">ahem</abbr><abbr title="Requires DOM/JS">dom/js</abbr></td>
                  </tr>
                </tbody>
              </table>
            </body>
          </html>
        scala.xml.XML.save(tocFile.getAbsolutePath, sampleToc, enc = "utf-8")

        val parser = new CssReftestXmlParser()
        val parsed = parser.parse(tocFile)
        val tests = parsed \ "test"
        tests.length should be(2)

        (tests(0) \ "test-title").text should be("Supported No Flags")
        (tests(0) \ "test-href").text should be("sup-none.xht")
        (tests(0) \ "reference-value").text should be("=")
        (tests(0) \ "reference-href").text should be("sup-none-ref.xht")
        (tests(0) \ "flags").isEmpty should be(true)

        (tests(1) \ "test-title").text should be("Supported Flags")
        (tests(1) \ "test-href").text should be("sup-flags.xht")
        (tests(1) \ "reference-value").text should be("!=")
        (tests(1) \ "reference-href").text should be("sup-flags-ref.xht")
        (tests(1) \ "flags" \ "flag").map(_.text) should be(Seq("ahem", "image"))
      } finally {
        better.files.File(tempDir.toPath).delete()
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

  describe("GngrDriver") {
    it("should close its socket cleanly when handshake fails") {
      import java.io.{BufferedReader, DataOutputStream, InputStreamReader}
      import java.net.{InetAddress, ServerSocket}
      import java.nio.file.Files

      val tempDir = Files.createTempDirectory("grinder-driver-socket-test").toFile
      val loopback = InetAddress.getByAddress(Array[Byte](127, 0, 0, 1))
      val server = new ServerSocket(0, 1, loopback)
      server.setSoTimeout(5000)

      @volatile var handshakeLine: String = null
      @volatile var ackLine: String = null
      @volatile var sawEofAfterClose = false
      @volatile var serverError: Option[Throwable] = None

      val serverThread = new Thread(() => {
        try {
          val client = server.accept()
          try {
            client.setSoTimeout(5000)
            val reader = new BufferedReader(new InputStreamReader(client.getInputStream, "UTF-8"))
            val dos = new DataOutputStream(client.getOutputStream)
            handshakeLine = reader.readLine()
            dos.writeInt(-1)
            dos.flush()
            ackLine = reader.readLine()
            sawEofAfterClose = reader.readLine() == null
          } finally {
            client.close()
          }
        } catch {
          case t: Throwable => serverError = Some(t)
        }
      })
      serverThread.setDaemon(true)
      serverThread.start()

      try {
        val portFile = new File(tempDir, "port.dat")
        val portOut = new DataOutputStream(Files.newOutputStream(portFile.toPath))
        try {
          portOut.writeInt(server.getLocalPort)
        } finally {
          portOut.close()
        }

        val ex = intercept[InvalidConfigurationException] {
          new GngrDriver("test-key", tempDir)
        }
        ex.getMessage should include("Auth key rejected")

        serverThread.join(5000)
        serverThread.isAlive should be(false)
        serverError should be(None)
        handshakeLine should startWith("GRINDER ")
        ackLine should be("ACK")
        sawEofAfterClose should be(true)
      } finally {
        server.close()
        better.files.File(tempDir.toPath).delete()
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

  describe("Report Directory and Artifact Emission") {
    it("should resolve default report directory when not specified") {
      val defaultBase = ReportConfig.resolveReportBaseDir(Map.empty)
      defaultBase should be(java.nio.file.Paths.get(Boot.UserDir, "grinder-report").normalize())
    }

    it("should resolve custom report directory when specified via --reportDir or --report-dir") {
      val customAbsolute = java.nio.file.Files.createTempDirectory("grinder-test-report").toAbsolutePath().normalize()
      try {
        val resolved1 = ReportConfig.resolveReportBaseDir(Map("reportDir" -> customAbsolute.toString))
        resolved1 should be(customAbsolute)

        val resolved2 = ReportConfig.resolveReportBaseDir(Map("report-dir" -> customAbsolute.toString))
        resolved2 should be(customAbsolute)

        val relativeResolved = ReportConfig.resolveReportBaseDir(Map("reportDir" -> "custom-report-out"))
        relativeResolved should be(java.nio.file.Paths.get(Boot.UserDir, "custom-report-out").normalize())
      } finally {
        java.nio.file.Files.deleteIfExists(customAbsolute)
      }
    }

    it("should fall back to default when reportDir option is empty or whitespace") {
      val fallback1 = ReportConfig.resolveReportBaseDir(Map("reportDir" -> ""))
      fallback1 should be(java.nio.file.Paths.get(Boot.UserDir, "grinder-report").normalize())

      val fallback2 = ReportConfig.resolveReportBaseDir(Map("reportDir" -> "   "))
      fallback2 should be(java.nio.file.Paths.get(Boot.UserDir, "grinder-report").normalize())
    }

    it("should create timestamped run directory with report.html, reportChanges.html, and screenshots folder") {
      val tempBase = java.nio.file.Files.createTempDirectory("grinder-run-test")
      try {
        val runPaths = ReportConfig.createRunPaths(tempBase)

        // Base directory and timestamped run directory must exist
        java.nio.file.Files.exists(runPaths.baseReportDir) should be(true)
        java.nio.file.Files.exists(runPaths.runDir) should be(true)
        runPaths.runDir.getParent should be(tempBase)

        // screenshots folder must exist
        java.nio.file.Files.exists(runPaths.screenshotsDir) should be(true)
        java.nio.file.Files.isDirectory(runPaths.screenshotsDir) should be(true)
        runPaths.screenshotsDir.getFileName.toString should be("screenshots")

        // report.html and reportChanges.html must be emitted
        val reportHtml = runPaths.runDir.resolve("report.html")
        val reportChangesHtml = runPaths.runDir.resolve("reportChanges.html")
        java.nio.file.Files.exists(reportHtml) should be(true)
        java.nio.file.Files.exists(reportChangesHtml) should be(true)

        val reportContent = new String(java.nio.file.Files.readAllBytes(reportHtml), "UTF-8")
        reportContent should include("Grinder reports")
        reportContent should include("results.json")

        val reportChangesContent = new String(java.nio.file.Files.readAllBytes(reportChangesHtml), "UTF-8")
        reportChangesContent should include("Grinder reports")
      } finally {
        // Cleanup test directory
        better.files.File(tempBase).delete()
      }
    }

    it("should set reportBaseDir correctly on Grinder instance") {
      val grinderDefault = new Grinder(Seq("gngr", "test-key"), Map.empty)
      grinderDefault.reportBaseDir should be(java.nio.file.Paths.get(Boot.UserDir, "grinder-report").normalize())

      val grinderCustom = new Grinder(Seq("gngr", "test-key"), Map("reportDir" -> "/tmp/custom-ci-reports"))
      grinderCustom.reportBaseDir should be(java.nio.file.Paths.get("/tmp/custom-ci-reports").normalize())
    }

    it("should keep report.html and resource report.html in sync with indexed lookups and batched fragments") {
      import java.nio.file.{Files, Paths}

      val rootReport = Files.readAllBytes(Paths.get(Boot.UserDir, "report.html"))
      val resourceReport = Files.readAllBytes(Paths.get(Boot.UserDir, "src", "main", "resources", "report.html"))
      rootReport should be(resourceReport)

      val content = new String(rootReport, "UTF-8")
      content should include("""querySelectorAll("test")""")
      content should include("new Map()")
      content should include("document.createDocumentFragment()")
      content should not include("baseRes.find(")
    }
  }

  describe("Pause and Quit") {
    it("should cancel the ProgressBar task and not leak non-daemon threads when quitting after pause") {
      import java.awt.image.BufferedImage
      import java.io.{ByteArrayOutputStream, StringReader}
      import java.util.Collections
      import java.util.concurrent.{Future => JFuture, ScheduledThreadPoolExecutor, TimeUnit}
      import javax.imageio.ImageIO
      import org.openqa.selenium.{By, Cookie, Dimension, OutputType, Point, TakesScreenshot, WebDriver, WebElement}
      import org.openqa.selenium.logging.Logs
      import scala.jdk.CollectionConverters._

      val tempReportDir = java.nio.file.Files.createTempDirectory("grinder-pause-quit-test")
      val dataDir = new File(s"${Boot.UserDir}/data")
      val testCasesFile = new File(dataDir, "test-cases.xml")
      val resultsJsonFile = new File(dataDir, "results.json")
      val resultsJsonExisted = resultsJsonFile.exists()
      val testCasesExisted = testCasesFile.exists()
      val pauseFile = new File(Pause.pauseFileLocation)
      val pauseBackup = Option.when(pauseFile.exists()) {
        val backup = java.nio.file.Files.createTempFile("grinder-pause-backup", ".tmp")
        java.nio.file.Files.move(pauseFile.toPath, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        backup
      }

      if (!testCasesExisted || (scala.xml.XML.loadFile(testCasesFile) \\ "test").isEmpty) {
        dataDir.mkdirs()
        val xml =
          <tests>
            <test id="test1.xht">
              <test-title>Test 1</test-title>
              <test-href>test1.xht</test-href>
              <reference-value>=</reference-value>
              <reference-href>test1-ref.xht</reference-href>
            </test>
          </tests>
        scala.xml.XML.save(testCasesFile.getAbsolutePath, xml, enc = "utf-8")
      }

      try {
        val utilClass = Class.forName("me.tongfei.progressbar.Util")
        val executorField = utilClass.getDeclaredField("executor")
        executorField.setAccessible(true)
        val executor = executorField.get(null).asInstanceOf[ScheduledThreadPoolExecutor]

        var threadsDuring = Set.empty[Thread]
        var activeTasksDuring = Seq.empty[JFuture[?]]
        var executorActiveDuring = false

        val stubDriver: WebDriver & TakesScreenshot = new WebDriver with TakesScreenshot {
          private val imgBytes = {
            val img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            val baos = new ByteArrayOutputStream()
            ImageIO.write(img, "png", baos)
            baos.toByteArray
          }
          private val stubWindow = new WebDriver.Window {
            def getPosition(): Point = new Point(0, 0)
            def getSize(): Dimension = new Dimension(800, 800)
            def maximize(): Unit = ()
            def fullscreen(): Unit = ()
            def setPosition(p: Point): Unit = ()
            def setSize(d: Dimension): Unit = ()
          }
          private val stubTimeouts = new WebDriver.Timeouts {
            def implicitlyWait(time: Long, unit: TimeUnit): WebDriver.Timeouts = this
            def pageLoadTimeout(time: Long, unit: TimeUnit): WebDriver.Timeouts = this
            def setScriptTimeout(time: Long, unit: TimeUnit): WebDriver.Timeouts = this
          }
          private val stubOptions = new WebDriver.Options {
            def addCookie(c: Cookie): Unit = ()
            def deleteAllCookies(): Unit = ()
            def deleteCookie(c: Cookie): Unit = ()
            def deleteCookieNamed(n: String): Unit = ()
            def getCookieNamed(n: String): Cookie = null
            def getCookies(): java.util.Set[Cookie] = Collections.emptySet()
            def ime(): WebDriver.ImeHandler = null
            def logs(): Logs = null
            def timeouts(): WebDriver.Timeouts = stubTimeouts
            def window(): WebDriver.Window = stubWindow
          }
          private val stubNavigation = new WebDriver.Navigation {
            def back(): Unit = ()
            def forward(): Unit = ()
            def refresh(): Unit = ()
            def to(url: java.net.URL): Unit = ()
            def to(url: String): Unit = {
              threadsDuring ++= Thread.getAllStackTraces().keySet().asScala.filter(_.isAlive)
              val queuedTasks = executor.getQueue.asScala.collect {
                case f: JFuture[?] if !f.isCancelled && !f.isDone => f
              }.toSeq
              activeTasksDuring ++= queuedTasks
              executorActiveDuring = executorActiveDuring || queuedTasks.nonEmpty || executor.getActiveCount > 0
              if (!pauseFile.exists()) {
                pauseFile.createNewFile()
              }
            }
          }
          def close(): Unit = ()
          def findElement(by: By): WebElement = null
          def findElements(by: By): java.util.List[WebElement] = Collections.emptyList()
          def get(url: String): Unit = ()
          def getCurrentUrl(): String = ""
          def getPageSource(): String = ""
          def getTitle(): String = ""
          def getWindowHandle(): String = ""
          def getWindowHandles(): java.util.Set[String] = Collections.emptySet()
          def manage(): WebDriver.Options = stubOptions
          def navigate(): WebDriver.Navigation = stubNavigation
          def quit(): Unit = ()
          def switchTo(): WebDriver.TargetLocator = null
          def getScreenshotAs[T](ot: OutputType[T]): T = ot.convertFromPngBytes(imgBytes)
        }

        val grinder = new Grinder(Seq("gngr", "test-key"), Map("reportDir" -> tempReportDir.toString)) {
          override lazy val driver: WebDriver & TakesScreenshot = stubDriver
        }

        val threadsBefore = Thread.getAllStackTraces().keySet().asScala.toSet
        intercept[QuitRequestedException] {
          scala.Console.withIn(new StringReader("q\n")) {
            grinder.run()
          }
        }

        val progressBarThreadsDuring = threadsDuring.filter(_.getName.contains("ProgressBar"))
        progressBarThreadsDuring should not be empty
        progressBarThreadsDuring.foreach(_.isDaemon should be(true))
        executorActiveDuring should be(true)

        activeTasksDuring.foreach { f =>
          (f.isCancelled || f.isDone) should be(true)
        }
        val remainingActiveTasks = executor.getQueue.asScala.filter {
          case f: JFuture[?] => !f.isCancelled && !f.isDone
          case _ => true
        }
        remainingActiveTasks should be(empty)

        val liveNonDaemonSpawnedThreads = (Thread.getAllStackTraces().keySet().asScala.toSet -- threadsBefore).filter { t =>
          t.isAlive && !t.isDaemon
        }
        liveNonDaemonSpawnedThreads should be(empty)

        val liveProgressBarThreads = Thread.getAllStackTraces().keySet().asScala.filter { t =>
          t.isAlive && t.getName.contains("ProgressBar")
        }
        liveProgressBarThreads.foreach(_.isDaemon should be(true))
      } finally {
        pauseFile.delete()
        pauseBackup.foreach { backup =>
          java.nio.file.Files.move(backup, pauseFile.toPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
        if (!resultsJsonExisted) resultsJsonFile.delete()
        if (!testCasesExisted) testCasesFile.delete()
        better.files.File(tempReportDir).delete()
      }
    }
  }

  describe("BaseLine") {
    it("should compute existingResults, newlyAdded, progressions, and regressions") {
      val tempDir = java.nio.file.Files.createTempDirectory("grinder-baseline-test")
      try {
        val baseDir = tempDir.resolve("base")
        val newDir = tempDir.resolve("new")
        java.nio.file.Files.createDirectories(baseDir)
        java.nio.file.Files.createDirectories(newDir)

        val baseJson =
          """{
            |  "css21-reftests": {
            |    "results": [
            |      {"id": "same-pass.xht", "pass": true},
            |      {"id": "same-fail.xht", "pass": false},
            |      {"id": "progressed.xht", "pass": false},
            |      {"id": "regressed.xht", "pass": true}
            |    ]
            |  }
            |}""".stripMargin
        val newJson =
          """{
            |  "css21-reftests": {
            |    "results": [
            |      {"id": "same-pass.xht", "pass": true},
            |      {"id": "same-fail.xht", "pass": false},
            |      {"id": "progressed.xht", "pass": true},
            |      {"id": "regressed.xht", "pass": false},
            |      {"id": "added.xht", "pass": true}
            |    ]
            |  }
            |}""".stripMargin

        java.nio.file.Files.writeString(baseDir.resolve("results.json"), baseJson)
        java.nio.file.Files.writeString(newDir.resolve("results.json"), newJson)

        val baseLine = new BaseLine(newDir.toString, baseDir.toString)
        baseLine.existingResults.map(_.id).toSet should be(Set("same-pass.xht", "same-fail.xht", "progressed.xht", "regressed.xht"))
        baseLine.newlyAdded.map(_.id) should be(Array("added.xht"))
        baseLine.progressions.map(_.id) should be(Array("progressed.xht"))
        baseLine.regressions.map(_.id) should be(Array("regressed.xht"))
      } finally {
        better.files.File(tempDir).delete()
      }
    }

    it("should copy selected regression screenshots in order during UpdateBaseLine.update") {
      import java.io.{ByteArrayOutputStream, PrintStream, StringReader}
      import java.nio.file.Files

      val tempDir = Files.createTempDirectory("grinder-update-baseline-test")
      try {
        val baseDir = tempDir.resolve("base")
        val newDir = tempDir.resolve("new")
        val baseShots = baseDir.resolve("screenshots")
        val newShots = newDir.resolve("screenshots")
        Files.createDirectories(baseShots)
        Files.createDirectories(newShots)

        val baseJson =
          """{
            |  "css21-reftests": {
            |    "results": [
            |      {"id": "reg1.xht", "pass": true},
            |      {"id": "reg2.xht", "pass": true},
            |      {"id": "reg3.xht", "pass": true}
            |    ]
            |  }
            |}""".stripMargin
        val newJson =
          """{
            |  "css21-reftests": {
            |    "results": [
            |      {"id": "reg1.xht", "pass": false},
            |      {"id": "reg2.xht", "pass": false},
            |      {"id": "reg3.xht", "pass": false}
            |    ]
            |  }
            |}""".stripMargin

        Files.writeString(baseDir.resolve("results.json"), baseJson)
        Files.writeString(newDir.resolve("results.json"), newJson)

        Seq("reg1.xht", "reg2.xht", "reg3.xht").foreach { id =>
          Files.write(baseShots.resolve(s"$id.png"), s"base-$id".getBytes("UTF-8"))
          Files.write(newShots.resolve(s"$id.png"), s"new-$id".getBytes("UTF-8"))
        }

        val outBytes = new ByteArrayOutputStream()
        val outStream = new PrintStream(outBytes, true, "UTF-8")
        try {
          scala.Console.withOut(outStream) {
            scala.Console.withIn(new StringReader("y\ns\ny\n")) {
              UpdateBaseLine.update(Array(newDir.toString, baseDir.toString))
            }
          }
        } finally {
          outStream.close()
        }

        new String(Files.readAllBytes(baseShots.resolve("reg1.xht.png")), "UTF-8") should be("new-reg1.xht")
        new String(Files.readAllBytes(baseShots.resolve("reg2.xht.png")), "UTF-8") should be("base-reg2.xht")
        new String(Files.readAllBytes(baseShots.resolve("reg3.xht.png")), "UTF-8") should be("new-reg3.xht")

        val updatedBaseResults = ResultParser.getResults(baseDir).map(r => r.id -> r.pass).toMap
        updatedBaseResults("reg1.xht") should be(false)
        updatedBaseResults("reg2.xht") should be(true)
        updatedBaseResults("reg3.xht") should be(false)

        val output = outBytes.toString("UTF-8")
        output should include("Copied:\nTestResult(false,reg1.xht)\nTestResult(false,reg3.xht)")
      } finally {
        better.files.File(tempDir).delete()
      }
    }

    it("should resolve and copy URL-encoded screenshots and update results.json across newlyAdded, progressions, and regressions") {
      import java.io.{ByteArrayOutputStream, PrintStream, StringReader}
      import java.net.URLEncoder
      import java.nio.file.Files
      import com.google.gson.JsonParser

      val tempDir = Files.createTempDirectory("grinder-encoded-baseline-test")
      try {
        val baseDir = tempDir.resolve("base")
        val newDir = tempDir.resolve("new")
        val baseShots = baseDir.resolve("screenshots")
        val newShots = newDir.resolve("screenshots")
        Files.createDirectories(baseShots)
        Files.createDirectories(newShots)

        val addedId = "support/added test.xht"
        val progressedId = "chapter-1/progressed.xht"
        val regressedId = "chapter-2/regressed.xht"

        val baseJson =
          s"""{
             |  "css21-reftests": {
             |    "selectedCount": 2,
             |    "results": [
             |      {"id": "$progressedId", "pass": false},
             |      {"id": "$regressedId", "pass": true}
             |    ]
             |  }
             |}""".stripMargin
        val newJson =
          s"""{
             |  "css21-reftests": {
             |    "selectedCount": 3,
             |    "results": [
             |      {"id": "$progressedId", "pass": true},
             |      {"id": "$regressedId", "pass": false},
             |      {"id": "$addedId", "pass": true}
             |    ]
             |  }
             |}""".stripMargin

        Files.writeString(baseDir.resolve("results.json"), baseJson)
        Files.writeString(newDir.resolve("results.json"), newJson)

        def encName(id: String): String = URLEncoder.encode(id, "UTF-8") + ".png"

        Files.write(baseShots.resolve(encName(progressedId)), "base-prog".getBytes("UTF-8"))
        Files.write(baseShots.resolve(encName(regressedId)), "base-reg".getBytes("UTF-8"))
        Files.write(newShots.resolve(encName(addedId)), "new-added".getBytes("UTF-8"))
        Files.write(newShots.resolve(encName(progressedId)), "new-prog".getBytes("UTF-8"))
        Files.write(newShots.resolve(encName(regressedId)), "new-reg".getBytes("UTF-8"))

        val baseLine = new BaseLine(newDir.toString, baseDir.toString)
        baseLine.resolveNewScreenshot(addedId) should be(newShots.resolve(encName(addedId)))
        baseLine.resolveBaseScreenshot(addedId) should be(baseShots.resolve(encName(addedId)))
        baseLine.resolveBaseScreenshot(progressedId) should be(baseShots.resolve(encName(progressedId)))

        val outBytes = new ByteArrayOutputStream()
        val outStream = new PrintStream(outBytes, true, "UTF-8")
        try {
          scala.Console.withOut(outStream) {
            scala.Console.withIn(new StringReader("y\ny\ny\n")) {
              UpdateBaseLine.update(Array(newDir.toString, baseDir.toString))
            }
          }
        } finally {
          outStream.close()
        }

        new String(Files.readAllBytes(baseShots.resolve(encName(addedId))), "UTF-8") should be("new-added")
        new String(Files.readAllBytes(baseShots.resolve(encName(progressedId))), "UTF-8") should be("new-prog")
        new String(Files.readAllBytes(baseShots.resolve(encName(regressedId))), "UTF-8") should be("new-reg")

        val updatedJson = new JsonParser().parse(Files.readString(baseDir.resolve("results.json"))).getAsJsonObject
        updatedJson.getAsJsonObject("css21-reftests").get("selectedCount").getAsInt should be(3)

        val afterUpdate = new BaseLine(newDir.toString, baseDir.toString)
        afterUpdate.newlyAdded should be(empty)
        afterUpdate.progressions should be(empty)
        afterUpdate.regressions should be(empty)
      } finally {
        better.files.File(tempDir).delete()
      }
    }

    it("should update all screenshots and results.json non-interactively with --yes and --all") {
      import java.io.{ByteArrayOutputStream, PrintStream, StringReader}
      import java.net.URLEncoder
      import java.nio.file.Files

      Seq("--yes", "--all").foreach { flag =>
        val tempDir = Files.createTempDirectory(s"grinder-auto-yes-test-${flag.stripPrefix("--")}")
        try {
          val baseDir = tempDir.resolve("base")
          val newDir = tempDir.resolve("new")
          val newShots = newDir.resolve("screenshots")
          Files.createDirectories(baseDir)
          Files.createDirectories(newShots)

          val addedId = "dir/added.xht"
          val progressedId = "dir/progressed.xht"
          val regressedId = "dir/regressed.xht"

          val baseJson =
            s"""{
               |  "css21-reftests": {
               |    "results": [
               |      {"id": "$progressedId", "pass": false},
               |      {"id": "$regressedId", "pass": true}
               |    ]
               |  }
               |}""".stripMargin
          val newJson =
            s"""{
               |  "css21-reftests": {
               |    "results": [
               |      {"id": "$progressedId", "pass": true},
               |      {"id": "$regressedId", "pass": false},
               |      {"id": "$addedId", "pass": true}
               |    ]
               |  }
               |}""".stripMargin

          Files.writeString(baseDir.resolve("results.json"), baseJson)
          Files.writeString(newDir.resolve("results.json"), newJson)

          def encName(id: String): String = URLEncoder.encode(id, "UTF-8") + ".png"

          Files.write(newShots.resolve(encName(addedId)), "new-added".getBytes("UTF-8"))
          Files.write(newShots.resolve(encName(progressedId)), "new-prog".getBytes("UTF-8"))
          Files.write(newShots.resolve(encName(regressedId)), "new-reg".getBytes("UTF-8"))

          val outBytes = new ByteArrayOutputStream()
          val outStream = new PrintStream(outBytes, true, "UTF-8")
          try {
            scala.Console.withOut(outStream) {
              scala.Console.withIn(new StringReader("")) {
                UpdateBaseLine.update(Array(newDir.toString, baseDir.toString, flag))
              }
            }
          } finally {
            outStream.close()
          }

          val baseShots = baseDir.resolve("screenshots")
          new String(Files.readAllBytes(baseShots.resolve(encName(addedId))), "UTF-8") should be("new-added")
          new String(Files.readAllBytes(baseShots.resolve(encName(progressedId))), "UTF-8") should be("new-prog")
          new String(Files.readAllBytes(baseShots.resolve(encName(regressedId))), "UTF-8") should be("new-reg")

          val afterUpdate = new BaseLine(newDir.toString, baseDir.toString)
          afterUpdate.newlyAdded should be(empty)
          afterUpdate.progressions should be(empty)
          afterUpdate.regressions should be(empty)
        } finally {
          better.files.File(tempDir).delete()
        }
      }
    }
  }

  describe("FileUtils") {
    it("should write bytes only when the file does not exist or its content differs") {
      val tempDir = java.nio.file.Files.createTempDirectory("grinder-fileutils-test")
      try {
        val target = tempDir.resolve("sample.bin")
        val initialBytes = Array[Byte](1, 2, 3, 4, 5)
        FileUtils.writeFile(target.toString, initialBytes)
        java.nio.file.Files.readAllBytes(target) should be(initialBytes)

        val oldTime = java.nio.file.attribute.FileTime.fromMillis(1000000L)
        java.nio.file.Files.setLastModifiedTime(target, oldTime)
        FileUtils.writeFile(target.toString, initialBytes.clone())
        java.nio.file.Files.getLastModifiedTime(target) should be(oldTime)
        java.nio.file.Files.readAllBytes(target) should be(initialBytes)

        val sameLengthDifferentBytes = Array[Byte](1, 2, 3, 4, 9)
        FileUtils.writeFile(target.toString, sameLengthDifferentBytes)
        java.nio.file.Files.readAllBytes(target) should be(sameLengthDifferentBytes)
        java.nio.file.Files.getLastModifiedTime(target).toMillis should be > oldTime.toMillis

        val updatedBytes = Array[Byte](9, 8, 7)
        FileUtils.writeFile(target.toString, updatedBytes)
        java.nio.file.Files.readAllBytes(target) should be(updatedBytes)
      } finally {
        better.files.File(tempDir).delete()
      }
    }
  }

}
