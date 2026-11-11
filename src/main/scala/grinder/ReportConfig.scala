package grinder

import java.io.File
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class ReportPaths(
  baseReportDir: Path,
  runDir: Path,
  screenshotsDir: Path
)

object ReportConfig {
  val DefaultDirName: String = "grinder-report"
  private val TimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

  def resolveReportBaseDir(options: Map[String, String]): Path = {
    val configuredOpt = options.get("reportDir")
      .orElse(options.get("report-dir"))
      .map(_.trim)
      .filter(_.nonEmpty)

    configuredOpt match {
      case Some(dir) =>
        val p = Paths.get(dir)
        if (p.isAbsolute) p.normalize()
        else Paths.get(Boot.UserDir, dir).normalize()
      case None =>
        Paths.get(Boot.UserDir, DefaultDirName).normalize()
    }
  }

  def createRunPaths(baseReportDir: Path): ReportPaths = {
    Files.createDirectories(baseReportDir)

    val baseTimestamp = LocalDateTime.now().format(TimestampFormatter)
    var runDir = baseReportDir.resolve(baseTimestamp)
    var counter = 1
    while (Files.exists(runDir)) {
      runDir = baseReportDir.resolve(s"${baseTimestamp}_$counter")
      counter += 1
    }
    Files.createDirectories(runDir)

    val screenshotsDir = runDir.resolve("screenshots")
    Files.createDirectories(screenshotsDir)

    emitReportFiles(runDir)

    ReportPaths(baseReportDir, runDir, screenshotsDir)
  }

  def emitReportFiles(runDir: Path): Unit = {
    copyReportFile("report.html", runDir.resolve("report.html"))
    copyReportFile("reportChanges.html", runDir.resolve("reportChanges.html"))

    // Copy test-cases.xml if available so report.html works self-contained
    val testCasesFile = Paths.get(Boot.UserDir, "data", "test-cases.xml")
    if (Files.exists(testCasesFile)) {
      try {
        Files.copy(testCasesFile, runDir.resolve("test-cases.xml"), StandardCopyOption.REPLACE_EXISTING)
      } catch {
        case _: Exception =>
      }
    }
  }

  private def copyReportFile(fileName: String, target: Path): Unit = {
    val workspaceFile = Paths.get(Boot.UserDir, fileName)
    val content = if (Files.exists(workspaceFile)) {
      new String(Files.readAllBytes(workspaceFile), "UTF-8")
    } else {
      val is = getClass.getResourceAsStream("/" + fileName)
      if (is != null) {
        val src = scala.io.Source.fromInputStream(is, "UTF-8")
        try src.mkString finally src.close()
      } else {
        ""
      }
    }

    if (content.nonEmpty) {
      // In emitted report, point default input to "." for immediate out-of-the-box loading
      val modified = content
        .replace("""value="data/"""", """value="."""")
        .replace("""value="data/changeTests/"""", """value="."""")
      Files.write(target, modified.getBytes("UTF-8"))
    }
  }
}
