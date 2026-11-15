package grinder

import java.io.File
import scala.xml.{Node, NodeSeq, XML}
import scala.xml.parsing.XhtmlParser

object XmlUtils {

  def loadXmlFromFile(file: File): NodeSeq = {
    val source = io.Source.fromFile(file)
    try {
      XhtmlParser(source)
    } finally {
      source.close()
    }
  }

  def saveXmlFile(fileName: String, xml: Node, targetDirectory: String): Unit = {
    val path = s"$targetDirectory/$fileName.xml"
    println(s"Writing tests to $path")
    XML.save(path, xml, enc = "utf-8")
  }
}
