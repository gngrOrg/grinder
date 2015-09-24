package utils

import java.io.{File, FileReader}

import scala.xml.{Elem, XML}

object XmlUtils {

  def loadXmlFromFile(file: File): Elem = {
    val reader = new FileReader(file)
    val elem: Elem = XML.load(reader)
    reader.close()
    elem
  }

  def saveXmlFile(fileName: String, xml: String, targetDirectory: String) {
    XML.save(s"$targetDirectory/$fileName.xml", XML.loadString(xml), enc = "utf-8")
  }
}
