# grinder
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

## Preparing the tests

```
sbt run prepare
```

This will parse and filter the tests from the css2.1 reference TOC. The details of the filtered tests are then
stored in `data/test-cases.xml`.


## Running the tests

```
sbt run compare <browser-name> <auth-key-for-gngr>
```

where `browser-name` can be either `firefox` or `gngr`.

When running with `gngr`, you need to set it up first:
   * Start `gngr` with the `-grinder-key=xyz` flag (substitute `xyz` with any key of your choice).
   * Provide the same key to the grinder run. For example: `sbt run compare gngr xyz`

This will open a new browser window, navigate to the tests and their references, screenshot them and compare the images.
