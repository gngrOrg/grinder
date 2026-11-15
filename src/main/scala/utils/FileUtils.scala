package grinder

import java.nio.file.{Files, Paths}
import java.util.Arrays

object FileUtils {

  // Writes only if file content differs
  def writeFile(fileName: String, bytes: Array[Byte]): Unit = {
    val path = Paths.get(fileName)
    val same = Files.exists(path) &&
      Files.size(path) == bytes.length &&
      Arrays.equals(Files.readAllBytes(path), bytes)

    if (!same) {
      Files.write(path, bytes)
    }
  }
}
