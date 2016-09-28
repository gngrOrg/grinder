package grinder

import java.nio.file.Path
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.client.methods.HttpPost

object ImgUpload {
  def uploadImgur(path: Path) {
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

    try {
      val responseXML = xml.XML.load(response.getEntity.getContent)
      println(responseXML \ "original_image")
      println(responseXML \ "imgur_page")
    } catch {
      case e:org.xml.sax.SAXParseException =>
        println("Exception while parsing result of upload")
    }
  }
}
