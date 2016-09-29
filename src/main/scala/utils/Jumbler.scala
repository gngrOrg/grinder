package grinder

object Jumbler {
  def jumbledUp[T](seq: Seq[T], seed: Long)(implicit tag: reflect.ClassTag[T]):Array[T] = {
    val arr = seq.toArray[T]
    val size = arr.length
    val numChanges = size / 2
    val rnd = new util.Random(seed)
    for (i <- 0 to numChanges) {
      val pos1 = rnd.nextInt(size)
      val pos2 = rnd.nextInt(size)
      swap(arr, pos1, pos2)
    }
    arr
  }

  private def swap[T](a: Array[T], pos1: Int, pos2: Int) {
    val t = a(pos1)
    a(pos1) = a(pos2)
    a(pos2) = t
  }
}
