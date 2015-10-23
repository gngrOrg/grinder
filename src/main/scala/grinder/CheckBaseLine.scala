package grinder

import java.nio.file.Path

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.impl.client.HttpClientBuilder

object CheckBaseLine {

  def check(args: Array[String]) {
    val baseLine = new BaseLine(args(0), args(1))

    {
      val progressions = baseLine.progressions
      println("Progressions: " + progressions.length)
      progressions.foreach { p => println(p.id) }
    }

    {
      val regressions = baseLine.regressions
      println("Regressions: " + regressions.length)
      if (regressions.length > 0) {
        println("-----")
        regressions.foreach { r => println(r.id) }
        println("-----")
        regressions.take(2).foreach { r => upload(r, baseLine) }
        System.exit(1)
      }
    }
  }

  private def upload(tr: ResultParser.TestResult, baseLine: BaseLine) {
    val newShot = baseLine.resolveNewScreenshot(tr.id)
    uploadImgur(newShot)
  }

  private def uploadImgur(path: Path) {
    println("Uploading: " + path)

    val fileBody = new FileBody(path.toFile())

    val builder = MultipartEntityBuilder.create()
    builder.addTextBody("key", "b3625162d3418ac51a9ee805b1840452")
    builder.addPart("image", fileBody)
    val entity = builder.build()

    val post = new HttpPost("https://imgur.com/api/upload.xml")
    post.setEntity(entity)

    val client = HttpClientBuilder.create().build()
    val response = client.execute(post)

    val responseXML = xml.XML.load(response.getEntity.getContent)
    println(responseXML \ "original_image")
    println(responseXML \ "imgur_page")
  }
}