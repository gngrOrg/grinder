# Optimisation Plan

1. [x] Replace O(N) baseline lookup per test in `Grinder.run` with an indexed map
2. [x] Replace O(N^2) `.exists` and `.contains` scans in `BaseLine` with map/set lookups
3. [x] Eliminate O(N^2) DOM queries, linear scans, and per-item DOM appends in `report.html`
4. [x] Cache decoded reference images in memory and avoid redundant disk reads after screenshot capture
5. [x] Replace single-byte read loops in `GrinderUtil.isFileContentSame` and `FileUtils.writeFile`
6. [x] Add early exit to the inner pixel comparison loop in `GrinderUtil.isScreenShotSame`
7. [x] Read screenshots directly into a pre-sized byte array in `GngrDriver.getScreenshotAs`
8. [x] Cache loopback `InetAddress` and close streams/sockets safely in `GngrDriver`
9. [x] Remove XML string round-trip in `Boot.prepare` / `XmlUtils.saveXmlFile` and close `Source` in `loadXmlFromFile`
10. [x] Avoid duplicate `\\` traversals and parse flags before other fields in `CssReftestXmlParser`
11. [x] Remove duplicate URL encoding in `Grinder` / `ChangeChecker` and O(N^2) list append in `UpdateBaseLine`
12. [x] Remove unused legacy classes (`parser.CssReftestXmlParser`, `utils.XmlUtils`) and narrow `-opt-inline`

## 1. Per-test baseline lookup in `Grinder.run`

Target: `src/main/scala/grinder/Grinder.scala` (lines 28-33, 127)

Issue: `baseLineResultsOpt` stores `Option[Array[ResultParser.TestResult]]` from `ResultParser.getResults(basePath)`. Inside `selectedTests.foreach`, line 127 calls `baseLineResultsOpt.flatMap(_.find(_.id == result.id))`, performing an O(N) linear scan for every test (O(N^2) across the full suite).

Change: Convert `ResultParser.getResults(basePath)` into `Map[String, ResultParser.TestResult]` (or `Map[String, Boolean]`) once when initializing `baseLineResultsOpt`, and replace `.find(_.id == result.id)` with `.get(result.id)` for O(1) lookups.

## 2. Quadratic filtering in `BaseLine`

Target: `src/main/scala/grinder/BaseLine.scala` (lines 11-20)

Issue: `baseResults` and `newResults` are `Array[ResultParser.TestResult]`. Line 15 runs `newResults.partition(n => baseResults.exists(_.id == n.id))` (O(N^2)). Lines 19-20 run `newPassed.filterNot(n => basePassed.contains(n))` and `newFailed.filterNot(n => baseFailed.contains(n))`, where `Array.contains` is also O(N).

Change: Index `baseResults` into a `Map[String, Boolean]` (`id -> pass`) once, then compute `existingResults`, `newlyAdded`, `progressions`, and `regressions` with O(1) map lookups in a single pass over `newResults`.

## 3. DOM queries and linear scans in `report.html`

Target: `report.html` (lines 157-167, 221-267) and `src/main/resources/report.html` (lines 157-167, 221-267)

Issue: Inside `dataRes.forEach` in `report()`, `findTestInfo` executes `testInfo.querySelector("test[id='"+id+"']")` across the XML document on every item (O(N^2)), and `baseRes.find((b) => b.id == dr.id)` performs a linear array scan on every item (O(N^2)). Each iteration also calls `document.getElementById(...).appendChild(...)`, mutating the live DOM repeatedly.

Change: Pre-index `testInfo` (`querySelectorAll("test")`) and `baseRes` into JavaScript `Map` objects keyed by `id` before the loop. Cache the target `<ol>` elements and build `<li>` nodes into `DocumentFragment`s, appending each fragment once after the loop. Keep both `report.html` and `src/main/resources/report.html` in sync.

## 4. Redundant disk I/O and repeated reference image decoding

Target: `src/main/scala/grinder/Grinder.scala` (lines 113-118, 242-260), `src/main/scala/grinder/ChangeChecker.scala` (lines 102-107, 205-228), `src/main/scala/utils/ImageCompare.scala` (lines 13-23)

Issue: `takeScreenShot` receives `bytes: Array[Byte]` from `driver.getScreenshotAs(OutputType.BYTES)`, writes the PNG to disk, and immediately `GrinderUtil.isScreenShotSame` reads the file back from disk twice (once in `isFileContentSame` and once via `ImageIO.read`). In addition, many CSS2.1 tests share the same `referenceHref`, causing the same reference PNG to be repeatedly read and decoded from disk.

Change: Maintain a bounded LRU cache (e.g., `java.util.LinkedHashMap` with `removeEldestEntry`) of decoded reference `BufferedImage`s (or raw PNG bytes) keyed by screenshot path/href so shared reference images are decoded once. Add an in-memory comparison overload or helper in `GrinderUtil` so freshly captured PNG bytes can be compared directly without re-reading from disk, while keeping `isScreenShotSame(File, File, Double)` for existing callers and tests.

## 5. Single-byte read loops in `isFileContentSame` and `FileUtils.writeFile`

Target: `src/main/scala/utils/ImageCompare.scala` (lines 85-109) and `src/main/scala/utils/FileUtils.scala` (lines 12-41)

Issue: `isFileContentSame` reads both `BufferedInputStream`s one byte at a time (`in1.read()`, `in2.read()`) and does not close streams inside `try / finally`. In `FileUtils.writeFile`, `val path = Paths.get(fileName+"x")` has a trailing `"x"` typo, so the existence check never matches, and when it would match it reads one byte at a time without closing the stream inside `try / finally`.

Change: Replace `isFileContentSame` with `java.nio.file.Files.mismatch(file1.toPath, file2.toPath) == -1L` (or bulk byte-array reads wrapped in `try / finally` if targeting older JDKs). In `FileUtils.writeFile`, fix `val path = Paths.get(fileName)` and check `Files.exists(path) && Files.size(path) == bytes.length && Arrays.equals(Files.readAllBytes(path), bytes)` before calling `Files.write(path, bytes)`.

## 6. Missing early exit in inner pixel loop

Target: `src/main/scala/utils/ImageCompare.scala` (lines 32-49)

Issue: The outer row loop checks `while (h < height && failures < scaledFailureThreshold)`, but the inner column loop (`while (w < width)`) keeps iterating through the rest of the row even after `failures` reaches `scaledFailureThreshold`.

Change: Add `&& failures < scaledFailureThreshold` to the inner `while` condition on line 41 so comparison stops immediately on the failing pixel.

## 7. Redundant buffer copies in `GngrDriver.getScreenshotAs`

Target: `src/main/scala/grinder/GngrDriver.scala` (lines 202-213, 226-243)

Issue: `getScreenshotAs` reads the exact payload length (`val fileSize = (new DataInputStream(is)).readInt()`) and then calls `readInputStream(is, fileSize)`. `readInputStream` allocates a `ByteArrayOutputStream` (32-byte initial capacity that grows and copies repeatedly), a 16,384-byte `Array[Byte]` chunk buffer, and copies again in `buffer.toByteArray()`.

Change: Allocate `val bytes = new Array[Byte](fileSize)` once and populate it directly with `dis.readFully(bytes)` on the `DataInputStream`, removing `readInputStream` and intermediate copies.

## 8. Per-command allocations and unclosed resources in `GngrDriver`

Target: `src/main/scala/grinder/GngrDriver.scala` (lines 105-108, 128-133, 147-156)

Issue: `getSocket` calls `InetAddress.getByAddress(Array[Byte](127, 0, 0, 1))` on every socket command. `getGngrPort` opens `FileInputStream` and `DataInputStream` without closing them. `execute` calls `socket.close()` outside a `try / finally`, leaking sockets if `responseHandler` throws.

Change: Cache `private val loopbackAddress = InetAddress.getLoopbackAddress` (or the 127.0.0.1 `InetAddress`) as a field on `GngrDriver`. Close `FileInputStream` / `DataInputStream` in `getGngrPort` and `socket` in `execute` inside `try / finally` blocks.

## 9. Double XML conversion in `Boot.prepare` and `XmlUtils`

Target: `src/main/scala/grinder/Boot.scala` (lines 56-62) and `src/main/scala/grinder/XmlUtils.scala` (lines 9-17)

Issue: `Boot.prepare` converts `testCaseXml: Elem` into a full `String` (`testCaseXml.toString()`), and `XmlUtils.saveXmlFile` immediately re-parses that `String` via `XML.loadString(xml)` before passing it to `XML.save`. In `XmlUtils.loadXmlFromFile`, `io.Source.fromFile(file)` is never closed.

Change: Update `XmlUtils.saveXmlFile` (or add an overload) to accept `scala.xml.Node` directly and pass it straight to `XML.save(path, node, enc = "utf-8")`. Close the `Source` in `loadXmlFromFile` using `try / finally`.

## 10. Repeated XML subtree traversals in `CssReftestXmlParser`

Target: `src/main/scala/grinder/CssReftestXmlParser.scala` (lines 10-41, 68-70)

Issue: `firstChild` uses descendant search `(n \\ tag).head` instead of direct child search (`\`). Per row, `td1.firstChild("a")` and `(td2 \\ "abbr")` are each evaluated twice. Title and href attributes are extracted and `TestCase` instances allocated even for rows that are immediately dropped by `.filterNot(hasUnsupportedFlag)`. `UNSUPPORTED_FLAGS` is a `List` instead of a `Set`, and `def hasUnsupportedFlag` allocates a closure when passed to `filterNot`.

Change: Change `UNSUPPORTED_FLAGS` to `Set("interact", "paged", "dom", "dom/js")`. Parse `<abbr>` flags from `td2` first in each row and skip unsupported rows before extracting `td0` and `td1`. Cache the `td1` `<a>` node and `td2` `<abbr>` results in local `val`s, and replace `\\` with `\` for direct children.

## 11. Duplicate URL encoding and O(N^2) list append

Target: `src/main/scala/grinder/Grinder.scala` (lines 114-117, 242-260), `src/main/scala/grinder/ChangeChecker.scala` (lines 104-106, 205-228), `src/main/scala/grinder/UpdateBaseLine.scala` (lines 54-65)

Issue: `Grinder.run` and `ChangeChecker.run` call `enc(...)` when building screenshot file paths and call `enc(path)` a second time inside `navAndSnap`. In `UpdateBaseLine.update`, `var copied = Seq[ResultParser.TestResult]()` appends via `copied :+= r`, which is O(N) per append on an immutable `List` (O(N^2) total).

Change: Pass the pre-encoded filename (or target file path) into `navAndSnap` so `URLEncoder.encode` runs once per unique page. Replace `var copied = Seq[ResultParser.TestResult]()` with `scala.collection.mutable.ListBuffer[ResultParser.TestResult]` (or prepend with `+:=` and reverse before printing).

## 12. Unused legacy classes and optimizer scope

Target: `src/main/scala/parser/CssReftestXmlParser.scala`, `src/main/scala/utils/XmlUtils.scala`, `build.sbt` (line 12)

Issue: `parser.CssReftestXmlParser` and `utils.XmlUtils` are unused legacy duplicates of `grinder.CssReftestXmlParser` and `grinder.XmlUtils`, yet `build.sbt` still configures `-opt-inline:grinder.**,utils.**,parser.**`.

Change: Confirm no references remain, delete `src/main/scala/parser/CssReftestXmlParser.scala` and `src/main/scala/utils/XmlUtils.scala`, and narrow `-opt-inline` in `build.sbt` to `grinder.**`.
