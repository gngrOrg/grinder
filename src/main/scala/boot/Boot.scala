package boot

import java.io.File

import parser.CssReftestXmlParser


object Boot extends App{
  lazy val UserDir = {
    val UserDirProperty = System.getProperty("user.dir")
    if (UserDirProperty == null) "" else UserDirProperty
  }

  val parser = new CssReftestXmlParser
  val xmlFile = new File(s"$UserDir/nightly-unstable/xhtml1/reftest-toc.xht")

  println(xmlFile.getAbsolutePath)

  parser.parse(xmlFile)

}
