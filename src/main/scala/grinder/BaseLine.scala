package grinder

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file._

class BaseLine(newPathStr: String, basePathStr: String) {
  val newPath = Paths.get(newPathStr).toAbsolutePath().normalize()
  val basePath = Paths.get(basePathStr).toAbsolutePath().normalize()
  println("New path: " + newPath)
  println("Base path: " + basePath)

  val baseResults = ResultParser.getResults(basePath)
  val (basePassed, baseFailed) = baseResults.partition(_.pass)
  private val basePassById = baseResults.map(r => r.id -> r.pass).toMap

  val newResults = ResultParser.getResults(newPath)
  val (existingResults, newlyAdded) = newResults.partition(n => basePassById.contains(n.id))

  val (newPassed, newFailed) = existingResults.partition(_.pass)

  val progressions = newPassed.filter(n => !basePassById(n.id))
  val regressions = newFailed.filter(n => basePassById(n.id))

  private def resolveScreenshot(rootPath: Path, id: String): Path = {
    val encodedName = URLEncoder.encode(id, StandardCharsets.UTF_8.name()) + ".png"
    val rawName = id + ".png"
    val candidates = Seq(
      rootPath.resolve("screenshots").resolve(encodedName),
      rootPath.resolve("screenshot").resolve(encodedName),
      rootPath.resolve("screenshots").resolve(rawName),
      rootPath.resolve("screenshot").resolve(rawName)
    )
    candidates.find(Files.exists(_)).getOrElse {
      if (Files.isDirectory(rootPath.resolve("screenshots"))) {
        rootPath.resolve("screenshots").resolve(encodedName)
      } else if (Files.isDirectory(rootPath.resolve("screenshot"))) {
        rootPath.resolve("screenshot").resolve(encodedName)
      } else {
        rootPath.resolve("screenshots").resolve(encodedName)
      }
    }
  }

  def resolveNewScreenshot(id: String): Path = resolveScreenshot(newPath, id)

  def resolveBaseScreenshot(id: String): Path = resolveScreenshot(basePath, id)
}
