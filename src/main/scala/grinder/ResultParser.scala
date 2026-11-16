package grinder

import java.nio.charset.StandardCharsets
import java.nio.file._
import com.google.gson.{GsonBuilder, JsonObject, JsonParser}

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

  /** Updates existing entries and appends newly accepted entries in `<basePath>/results.json`. */
  def updateResults(basePath: Path, updatedOrAdded: Seq[TestResult]): Unit = {
    if (updatedOrAdded.nonEmpty) {
      val myJSON = json(basePath)
      val reftestsObj = myJSON.getAsJsonObject("css21-reftests")
      val resultsArr = reftestsObj.getAsJsonArray("results")
      val updatesById = updatedOrAdded.map(r => r.id -> r.pass).toMap

      var seenIds = Set.empty[String]
      for (i <- 0 until resultsArr.size()) {
        val obj = resultsArr.get(i).getAsJsonObject
        val id = obj.get("id").getAsString
        seenIds += id
        updatesById.get(id).foreach { pass =>
          obj.addProperty("pass", java.lang.Boolean.valueOf(pass))
        }
      }

      updatedOrAdded.foreach { r =>
        if (!seenIds.contains(r.id)) {
          val itemObj = new JsonObject()
          itemObj.addProperty("id", r.id)
          itemObj.addProperty("pass", java.lang.Boolean.valueOf(r.pass))
          resultsArr.add(itemObj)
          seenIds += r.id
        }
      }

      if (reftestsObj.has("selectedCount")) {
        reftestsObj.addProperty("selectedCount", Integer.valueOf(resultsArr.size()))
      }

      val jsonFormatted = new GsonBuilder().setPrettyPrinting().create().toJson(myJSON)
      Files.write(basePath.resolve("results.json"), jsonFormatted.getBytes(StandardCharsets.UTF_8))
    }
  }

}
