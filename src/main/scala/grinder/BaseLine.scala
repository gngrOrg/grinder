package grinder

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

  def resolveNewScreenshot(id: String) = {
    val s1 = newPath.resolve("screenshots").resolve(id + ".png")
    if (Files.exists(s1)) s1 else newPath.resolve("screenshot").resolve(id + ".png")
  }

  def resolveBaseScreenshot(id: String) = {
    val s1 = basePath.resolve("screenshots").resolve(id + ".png")
    if (Files.exists(s1)) s1 else basePath.resolve("screenshot").resolve(id + ".png")
  }
}
