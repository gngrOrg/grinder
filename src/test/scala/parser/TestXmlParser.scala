package parser

import java.io.File

import utils.XmlUtils

class TestXmlParser {

  def parserTests = {
    val file = new File(s"${System.getProperty("user.dir")}/src/test/resources/test-cases.xml")
    val sourceXml = XmlUtils.loadXmlFromFile(file)

    val testCases = sourceXml \\ "test"

    testCases.par.map(test => {
      val title = (test \ "test-title").text
      val href = (test \ "test-href").text
      val refValue = (test \ "reference-value").text
      val refHref = (test \ "reference-href").text
      val flagTitle = (test \ "flag-title").text
      val flagValue = (test \ "flag-value").text

      TestCase(title, href: String, refValue, refHref, flagTitle, flagValue)
    }).toList

  }

}
