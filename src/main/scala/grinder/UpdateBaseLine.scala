package grinder

import java.nio.file._
import scala.collection.mutable.ListBuffer

object UpdateBaseLine {

  private def copyScreenshot(baseLine: BaseLine, id: String): Unit = {
    val fromPath = baseLine.resolveNewScreenshot(id)
    val toPath = baseLine.resolveBaseScreenshot(id)
    if (Files.exists(fromPath)) {
      Option(toPath.getParent).foreach(Files.createDirectories(_))
      Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  private def copyBatchIfConfirmed(
    label: String,
    items: Array[ResultParser.TestResult],
    baseLine: BaseLine,
    autoYes: Boolean,
    acceptedResults: ListBuffer[ResultParser.TestResult]
  ): Unit = {
    println(s"$label: ${items.length}")
    if (items.length > 0) {
      val confirmed = if (autoYes) {
        true
      } else {
        println("[yY] to copy files from new to baseline")
        val response = io.StdIn.readLine()
        response != null && response.matches("[yY]")
      }
      if (confirmed) {
        items.foreach { item =>
          copyScreenshot(baseLine, item.id)
          acceptedResults += item
        }
      }
    }
  }

  /** Updates baseline screenshots and `results.json` from a newer test run (`--yes` / `--all` skips prompts). */
  def update(args: Array[String]): Unit = {
    val (argsRem, optionsMap) = Boot.separateArgsOptions(args)
    val autoYes = optionsMap.contains("yes") || optionsMap.contains("all")
    val baseLine = new BaseLine(argsRem(0), argsRem(1))

    val numBasePassed = baseLine.basePassed.length
    val numBaseFailed = baseLine.baseFailed.length
    val numNewPassed = baseLine.newPassed.length
    val numNewFailed = baseLine.newFailed.length

    println(s"[base] passed: $numBasePassed,  failed: $numBaseFailed")
    println(s"[new] passed: $numNewPassed (${numNewPassed - numBasePassed}),  failed: $numNewFailed (${numNewFailed - numBaseFailed})")

    val acceptedResults = ListBuffer[ResultParser.TestResult]()

    copyBatchIfConfirmed("newlyAdded", baseLine.newlyAdded, baseLine, autoYes, acceptedResults)
    copyBatchIfConfirmed("Progressions", baseLine.progressions, baseLine, autoYes, acceptedResults)

    {
      val regressions = baseLine.regressions
      println("Regressions: " + regressions.length)
      if (regressions.length > 0) {
        val copied = ListBuffer[ResultParser.TestResult]()
        var continue = true
        val iter = regressions.iterator
        while (continue && iter.hasNext) {
          val r = iter.next()
          val response = if (autoYes) {
            "y"
          } else {
            println(s"regressions: $r [yY] to copy files from new to baseline. [sS] to Skip. Anything else to quit")
            io.StdIn.readLine()
          }
          if (response != null && response.matches("[yY]")) {
            copyScreenshot(baseLine, r.id)
            copied += r
            acceptedResults += r
          } else if (response != null && response.matches("[sS]")) {
            // do nothing
          } else {
            continue = false
          }
        }
        println("Copied:\n" + copied.mkString("\n"))
      }
    }

    ResultParser.updateResults(baseLine.basePath, acceptedResults.toSeq)
  }

}