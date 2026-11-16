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
sbt run prepare [inputDir]
```

This will parse and filter the tests from the css2.1 reference TOC (default input directory is `nightly-unstable`). The details of the filtered tests are then
stored in `data/test-cases.xml`.


## Running the tests

```
sbt run compare <browser-name> <auth-key-for-gngr>
```

where `browser-name` can be either `firefox` or `gngr`.

This will open a new browser window, navigate to the tests and their references, screenshot them and compare the images.

When running with `gngr`, you need to set it up first:
   * Start `gngr` with the `-grinder-key=xyz` flag (substitute `xyz` with any key of your choice).
   * Provide the same key to the grinder run. For example: `sbt run compare gngr xyz`
   * Optionally specify the profile directory with `--profileDir=<path>` (or `--profile=<name>`). For example: `sbt "run compare gngr xyz --profileDir=/path/to/profile"`
   * Optionally specify the report output directory with `--reportDir=<path>`. Default is `$PWD/grinder-report/`. A timestamped folder will be created inside with `report.html`, `reportChanges.html`, `results.json`, and the `screenshots/` directory. For example: `sbt "run compare gngr xyz --reportDir=/path/to/artifacts"`


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
