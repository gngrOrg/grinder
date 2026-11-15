package grinder

import java.io.File
import scala.xml.NodeSeq.seqToNodeSeq

case class TestCase(testTitle: String, testHref: String, refrenceValue: String, referenceHref: String, flagTitle: String, flags: Seq[String])

class CssReftestXmlParser {

  val UNSUPPORTED_FLAGS = Set("interact", "paged", "dom", "dom/js")

  private implicit class NodeConvenience(n: xml.Node) {
    def attrib(key: String): String = {
      n.attribute(key).head.text
    }

    def firstChild(tag: String): xml.Node = {
      (n \ tag).head
    }
  }

  def parse(reftestXmlFile: File) = {

    val refTestXml = XmlUtils.loadXmlFromFile(reftestXmlFile)
    val rows = refTestXml \\ "tbody"

    val testCases = rows.flatMap { row =>
      val trs = row \ "tr"
      val tdNodes = trs.head \ "td"
      val td2 = tdNodes(2)
      val abbrNodes = td2 \ "abbr"
      val flagValue = abbrNodes.map(_.text).mkString(" ")
      val flags = flagValue.split("\\s+").filter(_.length > 0).map(_.toLowerCase).toSeq
      if (flags.exists(UNSUPPORTED_FLAGS.contains)) {
        None
      } else {
        val td0 = tdNodes.head
        val td1 = tdNodes(1)
        val td1Anchor = td1.firstChild("a")
        val testTitle = td0.attrib("title")
        val testHref = td0.firstChild("a").attrib("href")
        val refrenceValue = td1Anchor.text
        val refrenceHref = td1Anchor.attrib("href")
        val flagTitle = abbrNodes.map(_.attrib("title")).mkString(" ")

        Some(TestCase(testTitle, testHref, refrenceValue, refrenceHref, flagTitle, flags))
      }
    }

    println("Supported test cases: " + testCases.length)

    val testCaseXml = <tests>
      {testCases.map(testCase => {
        <test id={testCase.testHref}>
          <test-title>{testCase.testTitle}</test-title>
          <test-href>{testCase.testHref}</test-href>
          <reference-value>{testCase.refrenceValue}</reference-value>
          <reference-href>{testCase.referenceHref}</reference-href>
          {
            if (testCase.flags.nonEmpty) {
              <flags>
              {
                testCase.flags.map { flag => <flag>{flag}</flag> }
              }
              </flags>
            } else scala.xml.NodeSeq.Empty
          }
        </test>
      })}
    </tests>

    testCaseXml
  }

  def hasUnsupportedFlag(testCase: TestCase): Boolean = {
    testCase.flags.exists(UNSUPPORTED_FLAGS.contains)
  }
}
