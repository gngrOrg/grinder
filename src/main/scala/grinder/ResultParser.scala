package grinder

import java.nio.file._
import collection.JavaConversions._
import collection.JavaConverters._

object ResultParser {
  case class TestResult(pass: Boolean, id: String)

  import rapture.json._
  import jsonBackends.jawn._

  private def json(path: Path):Json = {
    val resultPath = path.resolve("results.json")
    val str = Files.readAllLines(resultPath).asScala.mkString("")
    Json.parse(str)
  }

  def getResults(path: Path):Array[TestResult] = {
    val myJSON = json(path)
    val tests = myJSON.selectDynamic("css21-reftests")
    tests.results.as[Array[TestResult]]
  }

}
