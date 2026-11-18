# grinder [![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/gngrOrg/grinder)
an apparatus to test [gngr](https://github.com/UprootLabs/gngr).

## Status
We are just getting started. We are currently trying to run the css2.1 test suite. More test-suites are [planned for the
future](https://github.com/UprootLabs/grinder/issues/8).

The test code uses the WebDriver protocol to navigate and screenshot the browser. The screenshots
for the test and its reference are then compared.

On `gngr` side, we are implementing a subset of the WebDriver protocol and will then run grinder against it.

# HowTo Guide

## Getting started

* `git clone` this repo.
* Download [the CSS2.1 testsuite](http://test.csswg.org/suites/css21_dev/) from w3c. We use the `nightly-unstable` package and
  unzip it into the root folder of this repo.
* Setup [sbt](http://www.scala-sbt.org/)
* Run a local server on port 8000 in the root folder of this repo. Example: `python -m SimpleHTTPServer`.

## Preparing the tests

```
sbt "run prepare [inputDir]"
```

This will parse and filter the tests from `<inputDir>/reftest-toc.xht` (or `<inputDir>/xhtml1/reftest-toc.xht`; default input directory is `nightly-unstable`). You can also specify the directory with `--inputDir=<path>` or `--input-dir=<path>`. The details of the filtered tests are then stored in `data/test-cases.xml`.


## Running the tests

```
sbt "run compare <browser-name> [auth-key-for-gngr] [options]"
```

where `browser-name` can be either `firefox` or `gngr`.

This will open a new browser window, navigate to the tests and their references, screenshot them and compare the images.

When running with `gngr`, you need to set it up first:
   * Start `gngr` with the `-grinder-key=xyz` flag (substitute `xyz` with any key of your choice).
   * Provide the same key to the grinder run, either positionally or with `--key=<auth-key>`. For example: `sbt "run compare gngr xyz"`
   * Optionally specify the profile directory with `--profileDir=<path>` (or `--profile=<name>`). For example: `sbt "run compare gngr xyz --profileDir=/path/to/profile"`
   * Optionally specify the report output directory with `--reportDir=<path>` (or `--report-dir=<path>`). Default is `$PWD/grinder-report/`. A timestamped folder will be created inside with `report.html`, `reportChanges.html`, `results.json`, and the `screenshots/` directory. For example: `sbt "run compare gngr xyz --reportDir=/path/to/artifacts"`

Additional options for `compare`:
   * `--hrefFilter=<substring>`: Only run tests whose `href` contains `<substring>`.
   * `--failureScale=<number>`: Scale factor (default `1.0`) for the pixel-difference failure threshold (`25 * failureScale` differing pixels).
   * `--quitOnRegress`: Stop the test run immediately when a regression against `--baseLine` is encountered.


## Checking for visual changes

```
sbt "run checkChange <browser-name> [auth-key-for-gngr] [options]"
```

where `browser-name` can be either `firefox` or `gngr`.

This navigates to `.html` and `.xht` test pages under `changeTests/tests/` (served at `localhost:8000`) and compares their screenshots against reference screenshots in `changeTests/screenshot/`, writing `results.json` and `screenshots/` to the report directory.

Options for `checkChange`:
   * `--key=<auth-key>`: Auth key for `gngr` (alternative to the positional key argument).
   * `--profileDir=<path>` / `--profile=<name>`: Profile directory or name under `~/.gngr/`.
   * `--reportDir=<path>` / `--report-dir=<path>`: Report output directory (default `$PWD/grinder-report/`).
   * `--nameFilter=<substring>`: Only run tests whose filename contains `<substring>`.
   * `--failureScale=<number>`: Scale factor (default `1.0`) for the pixel-difference failure threshold.
   * `--quitOnChange`: Stop the run immediately when a visual change is detected.


## Benchmarking

```
sbt "run benchmark <url> --key=<auth-key> [--profileDir=<path>|--profile=<name>]"
```

Navigates `gngr` to `<url>` and takes a screenshot 6 times, printing per-iteration and average elapsed time in milliseconds.


## Checking and updating the baseline

You can pass `--baseLine=<path>` to `compare` to check a test run against an existing baseline run directory (for example, `./grinder-report/<yyyyMMdd-HHmmss>`):

```
sbt "run compare gngr xyz --baseLine=/path/to/baseline"
```

To check a completed test run against a baseline (for example, in CI):

```
sbt "run checkBase <newDir> <baseDir>"
```

This compares `<newDir>/results.json` against `<baseDir>/results.json`, prints the count and test IDs of all `progressions` and `regressions`, and exits with status code `1` if one or more regressions are found (and `0` otherwise).

To update an existing baseline directory from the results of a newer test run:

```
sbt "run updateBase <newDir> <baseDir>"
```

This compares `<newDir>/results.json` against `<baseDir>/results.json`, prompts you to accept `newlyAdded`, `progressions`, and individual `regressions`, copies the accepted screenshots (handling URL-encoded filenames) into `<baseDir>`, and updates `<baseDir>/results.json`.

To accept and apply all changes non-interactively without prompting, pass `--yes` or `--all`:

```
sbt "run updateBase <newDir> <baseDir> --yes"
```

Running `sbt "run updateBase"` with no arguments (or `--help`) displays usage help.

## Pausing the tests

```
touch pause
```

in this repo root will cause tests to pause.
