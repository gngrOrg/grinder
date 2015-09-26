package grinder.test

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

trait BrowserDriver extends Driver {
  implicit val driver: WebDriver  = new FirefoxDriver()
  driver.manage().window().maximize()
   setTimeout()
}
