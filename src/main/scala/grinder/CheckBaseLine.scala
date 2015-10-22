package grinder

import java.nio.file._

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
        regressions.foreach { r => println(r.id) }
        System.exit(1)
      }
    }
  }

}