# grinder
an apparatus to test [gngr](https://github.com/UprootLabs/gngr).

## Status
We are just getting started. We are currently trying to run the css2.1 test suite. More test-suites are [planned for the
future](https://github.com/UprootLabs/grinder/issues/8).

The test code uses the WebDriver protocol to navigate and screenshot the browser. The screenshots
for the test and its reference are then compared.

On `gngr` side, we are implementing a subset of the WebDriver protocol and will then run grinder against it.

## Getting started

* `git clone` this repo.
* Download [the CSS2.1 testsuite](http://test.csswg.org/suites/css21_dev/) from w3c. We use the `nightly-unstable`
    package as of now.
* Setup [sbt](http://www.scala-sbt.org/)

## Preparing the tests

```
sbt run prepare
```

This will parse and filter the tests from the css2.1 reference TOC. The details of the tests are stored in
`data/test-cases.xml`


## Running the tests

```
sbt run compare
```

This will start the browser (currently, Firefox) and run the tests via the WebDriver API.
