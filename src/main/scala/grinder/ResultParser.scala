package grinder

import java.nio.charset.StandardCharsets
import java.nio.file._
import com.google.gson.{JsonObject, JsonParser}

object ResultParser {
  case class TestResult(pass: Boolean, id: String)

  private def json(path: Path): JsonObject = {
    val resultPath = path.resolve("results.json")
    val str = new String(Files.readAllBytes(resultPath), StandardCharsets.UTF_8)
    new JsonParser().parse(str).getAsJsonObject
  }

  def getResults(path: Path): Array[TestResult] = {
    val myJSON = json(path)
    val resultsArr = myJSON.getAsJsonObject("css21-reftests").getAsJsonArray("results")
    Array.tabulate(resultsArr.size()) { i =>
      val obj = resultsArr.get(i).getAsJsonObject
      TestResult(obj.get("pass").getAsBoolean, obj.get("id").getAsString)
    }
  }

}
