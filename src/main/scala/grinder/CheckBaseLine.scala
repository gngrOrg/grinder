package grinder

import java.nio.file.Path

object CheckBaseLine {

  def check(args: Array[String]) {
    val baseLine = new BaseLine(args(0), args(1))

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

  private def upload(tr: ResultParser.TestResult, baseLine: BaseLine) {
    val newShot = baseLine.resolveNewScreenshot(tr.id)
    ImgUpload.uploadImgur(newShot)
  }
}