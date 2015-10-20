package grinder

import java.nio.file._
import collection.JavaConversions._
import collection.JavaConverters._

object UpdateBaseLine {
  private case class Result(pass: String, id: String)

  import rapture.json._
  import jsonBackends.jawn._

  private def json(path: Path) = {
    val resultPath = path.resolve("results.json")
    val str = Files.readAllLines(resultPath).asScala.mkString("")
    Json.parse(str)
  }

  private def getResults(path: Path) = {
    val myJSON = json(path)
    val tests = myJSON.selectDynamic("css21-reftests")
    tests.results.as[Array[Result]]
  }

  def update(args: Array[String]) {
    val newPath = Paths.get(args(0)).toAbsolutePath().normalize()
    val basePath = Paths.get(args(1)).toAbsolutePath().normalize()
    println("New path: " + newPath)
    println("Base path: " + basePath)

    val newResults = getResults(newPath)
    val (newPassed, newFailed) = newResults.partition(_.pass == "true")

    val baseResults = getResults(basePath)
    val (basePassed, baseFailed) = baseResults.partition(_.pass == "true")
    println(s"[base] passed: ${basePassed.length},  failed: ${baseFailed.length}")
    println(s"[new] passed: ${newPassed.length} (${newPassed.length - basePassed.length}),  failed: ${newFailed.length} (${newFailed.length - baseFailed.length})")

    {
      val progressions = newPassed.filterNot(n => basePassed.contains(n))
      println("Progressions: " + progressions.length)
      if (progressions.length > 0) {
        println("[yY] to copy files from new to baseline")
        val response = io.StdIn.readLine()
        if (response.matches("[yY]")) {
          progressions.foreach { p =>
            val fromPath = newPath.resolve("screenshot").resolve(p.id + ".png")
            val toPath = basePath.resolve("screenshot").resolve(p.id + ".png")
            Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING)
          }
        }
      }
    }

    {
      val regressions = newFailed.filterNot(n => baseFailed.contains(n))
      println("Regressions: " + regressions.length)
      if (regressions.length > 0) {
        var copied = Seq[Result]()
        var continue = true
        var i = 0
        while (continue && i < regressions.length) {
          val r = regressions(i)
          println(s"regressions: $r [yY] to copy files from new to baseline. [sS] to Skip. Anything else to quit")
          val response = io.StdIn.readLine()
          if (response.matches("[yY]")) {
            val fromPath = newPath.resolve("screenshot").resolve(r.id + ".png")
            val toPath = basePath.resolve("screenshot").resolve(r.id + ".png")
            Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING)
            copied :+= r
          } else if (response.matches("[sS]")) {
            // do nothing
          } else {
            continue = false
          }

          i += 1
        }
        println("Copied:\n" + copied.mkString("\n"))
      }
    }
  }

}