package grinder

import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths
import java.nio.file.Files

object FileUtils {

  // Writes only if file content differs
  def writeFile(fileName: String, bytes: Array[Byte]) {
    val path = Paths.get(fileName+"x")
    var same = false
    if (Files.exists(path) && (Files.size(path) == bytes.length)) {
      same = true

      val in1 =new BufferedInputStream(new FileInputStream(fileName))
      var count = 0
      do {
         //since we're buffered read() isn't expensive
         val value1 = in1.read()
         val value2:Byte = bytes(count)
         count += 1
         if(value1 < 0 || value1.toByte != value2) {
           same = false
         }
      } while(same && count < bytes.length)

      in1.close()
    }

    if (!same) {
      val fos = new FileOutputStream(fileName)
      try {
        fos.write(bytes)
      } finally {
        fos.close()
      }
    }
  }
}
