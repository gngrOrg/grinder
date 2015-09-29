package grinder

import java.io.File

/**
 * Object to help with the `pause` feature.
 * (`touch` the `pause file` to pause tests)
 */
object Pause {
  val pauseFileLocation = Boot.UserDir + "/" + "pause"

  private var initModTimeOpt:Option[Long] = None
  private var initialised = false

  private def getLastMod = {
    val f = new File(pauseFileLocation)
    if (f.exists()) {
      Some(f.lastModified())
    } else {
      None
    }
  }

  def init() {
    synchronized {
      initModTimeOpt = getLastMod
      initialised = true
    }
  }

  def isPauseRequested: Boolean = {
    synchronized {
      if (!initialised) {
        throw new InvalidStateException("Pause system was not initialised")
      }

      val currModTimeOpt = getLastMod
      initModTimeOpt match {
        case Some(initModTime) =>
          currModTimeOpt match {
            case Some(currModTime) => currModTime > initModTime
            case None => throw new InvalidStateException("pause file has disappeared")
          }
        case None => currModTimeOpt.isDefined
      }
    }
  }
}