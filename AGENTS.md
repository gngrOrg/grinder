# AGENTS.md — Grinder

WebDriver-based CSS2.1 reftest & visual regression harness for `gngr` and `firefox` (Scala 2.13.18, sbt 1.13.1).

## Guidelines
- When in doubt, ask.
- Do not inspect any files or processes outside the project directory. If absolutely necessary, explain the reason and plan, and ask before going ahead.

## Build & Commands
- `sbt test` — Run ScalaTest suite (`GrinderTest`).
- `sbt assembly` — Build fat JAR (`target/scala-2.13/grinder-assembly-*.jar`).
- `sbt "run <cmd> [args] [--opt=val]"` — Entrypoint `grinder.Boot`:
  - `prepare [inputDir]` — Parse `<inputDir>/[xhtml1/]reftest-toc.xht` (default `nightly-unstable`) -> `data/test-cases.xml`.
  - `compare <gngr|firefox> [key] [--profileDir=...] [--profile=...] [--reportDir=...] [--baseLine=...] [--hrefFilter=...] [--failureScale=...] [--quitOnRegress]` — Run CSS2.1 reftests.
  - `checkChange <gngr|firefox> [key] [--nameFilter=...] [--failureScale=...] [--quitOnChange]` — Compare `changeTests/tests/` against `changeTests/screenshot/`.
  - `checkBase <newDir> <baseDir>` / `updateBase <newDir> <baseDir>` — CI regression check (`exit 1` on regression) or interactive baseline screenshot copy.
  - `benchmark <url> --key=<key> [--profileDir=...]` — 6-iteration navigation + screenshot timer on `gngr`.

## Project Structure
- `src/main/scala/grinder/`
  - `Boot.scala` — CLI router, `UserDir` (`user.dir`), and `--key=val` / `--flag` parser (`separateArgsOptions`).
  - `Grinder.scala` / `ChangeChecker.scala` — Test runners: drive browser at `800x800`, deduplicate page visits (`visited: Set[String]`), screenshot URL-encoded filenames, compare images, emit `results.json`.
  - `GngrDriver.scala` — Custom `WebDriver with TakesScreenshot` client over loopback TCP (`127.0.0.1`).
  - `ReportConfig.scala` — Resolves `--reportDir` (default `./grinder-report`), creates `<yyyyMMdd-HHmmss>/` with `screenshots/`, `report.html`, `reportChanges.html`, `test-cases.xml`.
  - `BaseLine.scala`, `CheckBaseLine.scala`, `UpdateBaseLine.scala`, `ResultParser.scala` — Parse `results.json` and diff passes/fails into `progressions`, `regressions`, `newlyAdded`.
  - `CssReftestXmlParser.scala`, `TestParser.scala` (`TestXmlParser`), `XmlUtils.scala` — W3C XHTML TOC parser (filters out `interact`, `paged`, `dom`, `dom/js` flags) and XML loader.
  - `Pause.scala`, `Exceptions.scala` — Polls `./pause` file mtime for interactive pause/quit; `TestException` hierarchy (`InvalidConfigurationException`, `InvalidStateException`, `QuitRequestedException`).
- `src/main/scala/utils/` (compiled in `package grinder`)
  - `ImageCompare.scala` (`GrinderUtil`) — Byte-identical check, then 50-row batched RGBA diff (`|c1 - c2| < 10` per channel; fails if `>= 25 * failureScale` pixels differ).
  - `FileUtils.scala`, `Jumbler.scala` (seeded array shuffle, `seed = 0`), `Timer.scala`, `ImgUpload.scala` (no-op).
- `src/main/scala/{parser/CssReftestXmlParser.scala, utils/XmlUtils.scala}` — Legacy unused classes (`package parser` / `package utils`); active code uses `package grinder`.
- `report.html`, `reportChanges.html` (root & `src/main/resources/`) — Static HTML viewers loading `results.json` and `test-cases.xml`.
- `src/test/scala/grinder/GrinderTest.scala` & `src/test/resources/screenshot/` — Unit tests (`AnyFunSpec with Matchers`).

## Architecture & Coding Patterns
- **Single-Package Layout**: Both `src/main/scala/grinder/` and `src/main/scala/utils/` declare `package grinder` (except legacy `utils/XmlUtils.scala` and `parser/`).
- **Local HTTP & File Conventions**: Runners expect a static server at `localhost:8000` serving `nightly-unstable/xhtml1` or `changeTests`. Paths resolve relative to `Boot.UserDir`. Screenshot filenames are `URLEncoder.encode(href, "UTF-8") + ".png"`.
- **Gngr TCP Protocol (`GngrDriver`)**: Reads control port (`readInt`) from `<profileDir>/port.dat` (default `~/.gngr/default`). Handshakes with `GRINDER <Base64(SHA-256(key + port))>\r\n` to obtain session port + `ACK\r\n`. Sends short-lived socket commands (`SET_SIZE w h`, `TO url`, `SCREENSHOT`, `CLOSE`, `QUIT`) framed by `DataInputStream.readInt` status/length and `ACK\r\n`. Unused `WebDriver` methods are `???`.
- **Libraries & Idioms**:
  - Scala 2.13 style: singleton `object` utilities, `synchronized` state in `Pause`/`Timer`.
  - JSON via `com.google.gson` (`GsonBuilder`, `JsonObject`, `JsonArray`, `JsonParser`); XML via `scala.xml` literals and `XhtmlParser`; files via `java.nio.file`, `java.io.File`, and `better.files`.
- **Resource Safety & Testability**: `ProgressBar.close()` and `driver.quit()` are strictly wrapped in `try / finally` so early exits (`QuitRequestedException`) never leak threads or browser processes. Tests inject stub drivers by overriding `lazy val driver` on `Grinder`.
