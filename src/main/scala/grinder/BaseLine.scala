package grinder

import java.nio.file._

class BaseLine(newPathStr: String, basePathStr: String) {
  val newPath = Paths.get(newPathStr).toAbsolutePath().normalize()
  val basePath = Paths.get(basePathStr).toAbsolutePath().normalize()
  println("New path: " + newPath)
  println("Base path: " + basePath)

  val baseResults = ResultParser.getResults(basePath)
  val (basePassed, baseFailed) = baseResults.partition(_.pass)

  val newResults = ResultParser.getResults(newPath)
  val (existingResults, newlyAdded) = newResults.partition(n => baseResults.exists(_.id == n.id))

  val (newPassed, newFailed) = existingResults.partition(_.pass)

  val progressions = newPassed.filterNot(n => basePassed.contains(n))
  val regressions = newFailed.filterNot(n => baseFailed.contains(n))

  def resolveNewScreenshot(id: String) = {
    newPath.resolve("screenshot").resolve(id + ".png")
  }

  def resolveBaseScreenshot(id: String) = {
    basePath.resolve("screenshot").resolve(id + ".png")
  }
}
