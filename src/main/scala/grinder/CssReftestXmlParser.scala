package grinder

import java.io.File
import scala.xml.NodeSeq.seqToNodeSeq

case class TestCase(testTitle: String, testHref: String, refrenceValue: String, referenceHref: String, flagTitle: String, flags: Seq[String])

class CssReftestXmlParser {

  val UNSUPPORTED_FLAGS = List("interact","paged", "dom", "dom/js")

  private implicit def node2Convenience(n:xml.Node) = new {
    def attrib(key: String):String = {
      n.attribute(key).head.text
    }

    def firstChild(tag: String) = {
      (n \\ tag).head
    }
  }

  def parse(reftestXmlFile: File) = {

    val refTestXml = XmlUtils.loadXmlFromFile(reftestXmlFile)
    val rows = refTestXml \\ "tbody"

    val testCases = rows.map(row => {
      val trs = row \\ "tr"
      val tdNodes = trs.head \\ "td"
      val td0 = tdNodes.head
      val td1 = tdNodes(1)
      val td2 = tdNodes(2)
      val testTitle = td0.attrib("title")
      val testHref = td0.firstChild("a").attrib("href")
      val refrenceValue = td1.firstChild("a").text
      val refrenceHref = td1.firstChild("a").attrib("href")
      val flagTitle = (td2 \\ "abbr").map(node => node.attrib("title")).mkString(" ")
      val flagValue = (td2 \\ "abbr").map(node => node.head.text).mkString(" ")

      TestCase(testTitle, testHref, refrenceValue, refrenceHref, flagTitle, flagValue.split("\\s+").filter(_.length > 0).map(_.toLowerCase))
    }).filterNot(hasUnsupportedFlag)

    println("Supported test cases: " + testCases.length)

    val testCaseXml = <tests>
      {testCases.map(testCase => {
        <test id={testCase.testHref}>
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

    testCaseXml
  }

  def hasUnsupportedFlag: (TestCase) => Boolean = {
    testCase => UNSUPPORTED_FLAGS.exists(flag => testCase.flags.contains(flag))
  }
}
