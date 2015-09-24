package utils


import org.openqa.selenium.logging.{LogEntry, LogEntries, LogType}
import org.openqa.selenium.{JavascriptExecutor, By, WebElement}
import com.thoughtworks.selenium.Wait
import scala.collection.JavaConversions._

trait WaitAndGetElement {
  this: Driver =>
  def waitAndGet(locator: By): WebElement = {
    new Wait(s"Could Not Find element: $locator") {
      def until(): Boolean = driver.findElements(locator).size() != 0
    }
    driver.findElement(locator)
  }

  def waitAndGetElements(locator: By): List[WebElement] = {
    new Wait(s"Could Not Find element: $locator") {
      def until(): Boolean = driver.findElements(locator).size() != 0
    }
    driver.findElements(locator).toList
  }

}