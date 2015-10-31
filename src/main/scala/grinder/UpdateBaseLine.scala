package grinder

import java.nio.file._

object UpdateBaseLine {

  def update(args: Array[String]) {
    val baseLine = new BaseLine(args(0), args(1))

    val numBasePassed = baseLine.basePassed.length
    val numBaseFailed = baseLine.baseFailed.length
    val numNewPassed = baseLine.newPassed.length
    val numNewFailed = baseLine.newFailed.length

    println(s"[base] passed: $numBasePassed,  failed: $numBaseFailed")
    println(s"[new] passed: $numNewPassed (${numNewPassed - numBasePassed}),  failed: $numNewFailed (${numNewFailed - numBaseFailed})")

    {
      val newlyAdded = baseLine.newlyAdded
      println("newlyAdded: " + newlyAdded.length)
      if (newlyAdded.length > 0) {
        println("[yY] to copy files from new to baseline")
        val response = io.StdIn.readLine()
        if (response.matches("[yY]")) {
          newlyAdded.foreach { p =>
            val fromPath = baseLine.resolveNewScreenshot(p.id)
            val toPath = baseLine.resolveBaseScreenshot(p.id)
            Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING)
          }
        }
      }
    }

    {
      val progressions = baseLine.progressions
      println("Progressions: " + progressions.length)
      if (progressions.length > 0) {
        println("[yY] to copy files from new to baseline")
        val response = io.StdIn.readLine()
        if (response.matches("[yY]")) {
          progressions.foreach { p =>
            val fromPath = baseLine.resolveNewScreenshot(p.id)
            val toPath = baseLine.resolveBaseScreenshot(p.id)
            Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING)
          }
        }
      }
    }

    {
      val regressions = baseLine.regressions
      println("Regressions: " + regressions.length)
      if (regressions.length > 0) {
        var copied = Seq[ResultParser.TestResult]()
        var continue = true
        var i = 0
        while (continue && i < regressions.length) {
          val r = regressions(i)
          println(s"regressions: $r [yY] to copy files from new to baseline. [sS] to Skip. Anything else to quit")
          val response = io.StdIn.readLine()
          if (response.matches("[yY]")) {
            val fromPath = baseLine.resolveNewScreenshot(r.id)
            val toPath = baseLine.resolveBaseScreenshot(r.id)
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