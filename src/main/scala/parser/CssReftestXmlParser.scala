package parser

import java.io.File

import utils.XmlUtils

case class TestCase(testTitle: String, testHref: String, refrenceValue: String, referenceHref: String, flagTitle: String, flags: Seq[String])

class CssReftestXmlParser {

  val UNSUPPORTED_FLAGS = List("Interact","Paged")

  def parse(reftestXmlFile: File) = {

    val refTestXml = XmlUtils.loadXmlFromFile(reftestXmlFile)
    val rows = refTestXml \\ "tbody"

    val testCases = rows.map(row => {
      val tdNodes = row \ "tr" \\ "td"
      val testTitle = tdNodes.head.attribute("title").head.text
      val testHref = (tdNodes.head \\ "a").head.attribute("href").head.text
      val refrenceValue = (tdNodes(1) \ "a").head.text
      val refrenceHref = (tdNodes(1) \ "a").head.attribute("href").head.text
      val flagTitle = (tdNodes(2) \\ "abbr").map(node => node.attribute("title").head.text).mkString(" ")
      val flagValue = (tdNodes(2) \\ "abbr").map(node => node.head.text).mkString(" ")

      TestCase(testTitle, testHref, refrenceValue, refrenceHref, flagTitle, flagValue.split("\\s+").filter(_.length > 0))
    }).filterNot(hasUnsupportedFlag)

    println("Supported test cases: " + testCases.length)

    val testCaseXml = <tests>
      {testCases.map(testCase => {
        <test>
          <test-title>{testCase.testTitle}</test-title>
          <test-href>{testCase.testHref}</test-href>
          <reference-value>{testCase.refrenceValue}</reference-value>
          <reference-href>{testCase.referenceHref}</reference-href>
          {
            if (!testCase.flags.isEmpty) {
              <flags>
              {
                testCase.flags.map { flag => <flag>{flag}</flag> }
              }
              </flags>
            }
          }
        </test>
      })}
    </tests>

    XmlUtils.saveXmlFile("test-cases",testCaseXml.toString(), boot.Boot.UserDir + "/data/")

  }

  def hasUnsupportedFlag: (TestCase) => Boolean = {
    testCase => UNSUPPORTED_FLAGS.exists(flag => testCase.flags.contains(flag))
  }
}
