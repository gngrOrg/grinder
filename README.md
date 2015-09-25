# grinder
an apparatus to test [gngr](https://github.com/UprootLabs/gngr).

## Getting started

* `git clone` this repo.
* Download [the CSS2.1 testsuite](http://test.csswg.org/suites/css21_dev/) from w3c. We use the `nightly-unstable`
    package as of now.
* Setup [sbt](http://www.scala-sbt.org/)

## Preparing the tests

```
sbt run
```

This will parse and filter the tests from the css2.1 reference TOC. The details of the tests are stored in
`data/test-cases.xml`


## Running the tests

```
sbt test
```

This will start the browser (currently, your default browser) and run the tests via the WebDriver API.
