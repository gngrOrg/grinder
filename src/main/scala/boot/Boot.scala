package boot

import java.io.File

import parser.CssReftestXmlParser


object Boot extends App{

  val parser = new CssReftestXmlParser
  val xmlFile = new File(s"${System.getProperty("user.dir")}/src/main/resources/reftest-toc.xml")

  println(xmlFile.getAbsolutePath)

  parser.parse(xmlFile)

}
