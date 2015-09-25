package utils

import java.io.{File, FileReader}

import scala.xml.{Elem, XML, NodeSeq}
import scala.xml.parsing.XhtmlParser

object XmlUtils {

  def loadXmlFromFile(file: File): NodeSeq = {
    XhtmlParser(io.Source.fromFile(file))
  }

  def saveXmlFile(fileName: String, xml: String, targetDirectory: String) {
    val path = s"$targetDirectory/$fileName.xml"
    println(s"Writing tests to $path")
    XML.save(path, XML.loadString(xml), enc = "utf-8")
  }
}
