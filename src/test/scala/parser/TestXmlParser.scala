package parser

import java.io.File
import grinder.XmlUtils
import grinder.TestCase

class TestXmlParser {

  def parserTests = {
    val file = new File(s"${grinder.Boot.UserDir}/data/test-cases.xml")
    val sourceXml = XmlUtils.loadXmlFromFile(file)

    val testCases = sourceXml \\ "test"

    testCases.par.map(test => {
      val title = (test \ "test-title").text
      val href = (test \ "test-href").text
      val refValue = (test \ "reference-value").text
      val refHref = (test \ "reference-href").text
      val flagTitle = (test \ "flag-title").text
      val flags = (test \ "flags" \\ "flag").map(_.text)

      TestCase(title, href: String, refValue, refHref, flagTitle, flags)
    }).toList

  }

}
