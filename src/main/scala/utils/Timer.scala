package grinder

class Timer {
  private var consumedTime = 0L
  private var startTime = 0L
  private var running = false

  def start(): Unit = {
    synchronized {
      running = true
      startTime = System.currentTimeMillis()
    }
  }

  def stop(): Unit = {
    synchronized {
      consumedTime += System.currentTimeMillis() - startTime
      running = false
    }
  }

  def getConsumedTime: Long = {
    synchronized {
      if (running) {
        throw new IllegalStateException("Time being evaluated while running")
      }
      consumedTime
    }
  }
}