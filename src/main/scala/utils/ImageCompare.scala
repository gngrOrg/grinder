package grinder

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, File}
import java.nio.file.Files
import java.util.{Arrays, LinkedHashMap, Map => JMap}

import javax.imageio.ImageIO

object GrinderUtil {
  private val FAILURE_THRESHOLD = 25
  private val COMPARISION_THRESHOLD = 10
  private val ROWS_PER_BATCH = 50
  private val MAX_CACHE_ENTRIES = 64

  private final class CachedImage(
    val lastModified: Long,
    val length: Long,
    val bytes: Array[Byte],
    @volatile var decoded: BufferedImage
  )

  private val refCache = new LinkedHashMap[String, CachedImage](MAX_CACHE_ENTRIES, 0.75f, true) {
    override protected def removeEldestEntry(eldest: JMap.Entry[String, CachedImage]): Boolean =
      size() > MAX_CACHE_ENTRIES
  }

  @volatile private var cacheHits: Long = 0L

  private[grinder] def refCacheHits: Long = cacheHits

  private[grinder] def isRefDecodedCached(refImageFile: File): Boolean = {
    val key = refImageFile.getAbsolutePath
    refCache.synchronized {
      val entry = refCache.get(key)
      entry != null && entry.decoded != null
    }
  }

  private[grinder] def clearRefCache(): Unit = {
    refCache.synchronized {
      refCache.clear()
      cacheHits = 0L
    }
  }

  def isScreenShotSame(testFile: File, refImageFile: File, failureScale: Double = 1d): Boolean =
    isScreenShotSame(testFile, refImageFile, failureScale, None, None)

  def isScreenShotSame(
    testFile: File,
    refImageFile: File,
    failureScale: Double,
    testBytesOpt: Option[Array[Byte]],
    refBytesOpt: Option[Array[Byte]]
  ): Boolean = {
    val scaledFailureThreshold = FAILURE_THRESHOLD * failureScale

    if (!((testBytesOpt.isDefined || testFile.exists()) && (refBytesOpt.isDefined || refImageFile.exists()))) {
      false
    } else {
      val testBytes = testBytesOpt.getOrElse(Files.readAllBytes(testFile.toPath))
      val refEntry = getOrLoadRef(refImageFile, refBytesOpt)

      if (Arrays.equals(testBytes, refEntry.bytes)) {
        true
      } else {
        val testImage = ImageIO.read(new ByteArrayInputStream(testBytes))
        val referenceImage = getDecodedImage(refEntry)

        if (testImage != null && referenceImage != null && hasEqualDimensions(testImage, referenceImage)) {
          val width = testImage.getWidth
          val height = testImage.getHeight
          val testRGBArray = new Array[Int](width * ROWS_PER_BATCH)
          val refRGBArray = new Array[Int](width * ROWS_PER_BATCH)
          var failures = 0
          var h = 0
          var batchStart = 0
          while (h < height && failures < scaledFailureThreshold) {
            if (h % ROWS_PER_BATCH == 0) {
              batchStart = h
              val batchHeight = Math.min(height - batchStart, ROWS_PER_BATCH)
              testImage.getRGB(0, batchStart, width, batchHeight, testRGBArray, 0, width)
              referenceImage.getRGB(0, batchStart, width, batchHeight, refRGBArray, 0, width)
            }
            val offset = (h - batchStart) * width
            var w = 0
            while (w < width && failures < scaledFailureThreshold) {
              val same = isPixelSimilar(testRGBArray(w + offset), refRGBArray(w + offset))
              if (!same) {
                failures += 1
              }
              w += 1
            }
            h += 1
          }
          failures < scaledFailureThreshold
        } else {
          false
        }
      }
    }
  }

  private def getOrLoadRef(refImageFile: File, refBytesOpt: Option[Array[Byte]]): CachedImage = {
    val key = refImageFile.getAbsolutePath
    refBytesOpt match {
      case Some(refBytes) =>
        val lastMod = refImageFile.lastModified()
        refCache.synchronized {
          val existing = refCache.get(key)
          if (existing != null && existing.length == refBytes.length && Arrays.equals(existing.bytes, refBytes)) {
            cacheHits += 1L
            if (existing.lastModified == lastMod) {
              existing
            } else {
              val updated = new CachedImage(lastMod, refBytes.length.toLong, refBytes, existing.decoded)
              refCache.put(key, updated)
              updated
            }
          } else {
            val entry = new CachedImage(lastMod, refBytes.length.toLong, refBytes, null)
            refCache.put(key, entry)
            entry
          }
        }
      case None =>
        val lastMod = refImageFile.lastModified()
        val len = refImageFile.length()
        val cached: CachedImage = refCache.synchronized {
          val existing = refCache.get(key)
          if (existing != null && existing.lastModified == lastMod && existing.length == len) {
            cacheHits += 1L
            existing
          } else {
            null
          }
        }
        if (cached != null) {
          cached
        } else {
          val bytes = Files.readAllBytes(refImageFile.toPath)
          val entry = new CachedImage(lastMod, len, bytes, null)
          refCache.synchronized {
            refCache.put(key, entry)
          }
          entry
        }
    }
  }

  private def getDecodedImage(entry: CachedImage): BufferedImage = {
    val img = entry.decoded
    if (img != null) {
      img
    } else {
      entry.synchronized {
        val current = entry.decoded
        if (current != null) {
          current
        } else {
          val decoded = ImageIO.read(new ByteArrayInputStream(entry.bytes))
          entry.decoded = decoded
          decoded
        }
      }
    }
  }

  private def isPixelSimilar(p1: Int, p2: Int): Boolean = {
    if (p1 == p2) {
      true
    } else {
      val r1 = (p1 >> 24) & 0xff;
      val g1 = (p1 >> 16) & 0xff;
      val b1 = (p1 >> 8) & 0xff;
      val a1 = (p1) & 0xff;

      val r2 = (p2 >> 24) & 0xff;
      val g2 = (p2 >> 16) & 0xff;
      val b2 = (p2 >> 8) & 0xff;
      val a2 = (p2) & 0xff;
      similar(r1, r2) && similar(g1, g2) && similar(b1, b2) && similar(a1, a2)
    }
  }

  private def similar(i1:Int, i2:Int): Boolean = {
    Math.abs(i1 - i2) < COMPARISION_THRESHOLD
  }

  private def hasEqualDimensions(testImage: BufferedImage, referenceImage: BufferedImage): Boolean = {
    testImage.getWidth == referenceImage.getWidth && testImage.getHeight == referenceImage.getHeight
  }

}
