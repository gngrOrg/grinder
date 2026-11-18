package grinder

import java.nio.file.Path

object CheckBaseLine {

  /** Prints usage help for the `checkBase` command. */
  def printHelp(): Unit = {
    println("Usage: checkBase <newDir> <baseDir>")
    println("  <newDir>   Path to the new test run directory containing results.json")
    println("  <baseDir>  Path to the baseline directory containing results.json")
  }

  def check(args: Array[String]): Unit = {
    val (argsRem, optionsMap) = Boot.separateArgsOptions(args)
    if (argsRem.length < 2 || optionsMap.contains("help")) {
      printHelp()
    } else {
      val baseLine = new BaseLine(argsRem(0), argsRem(1))

      {
        val progressions = baseLine.progressions
        println("Progressions: " + progressions.length)
        progressions.foreach { p => println(p.id) }
      }

      {
        val regressions = baseLine.regressions
        println("Regressions: " + regressions.length)
        if (regressions.length > 0) {
          println("-----")
          regressions.foreach { r => println(r.id) }
          println("-----")
          regressions.take(2).foreach { r => upload(r, baseLine) }
          System.exit(1)
        }
      }
    }
  }

  private def upload(tr: ResultParser.TestResult, baseLine: BaseLine): Unit = {
    val newShot = baseLine.resolveNewScreenshot(tr.id)
    ImgUpload.uploadImgur(newShot)
  }
}