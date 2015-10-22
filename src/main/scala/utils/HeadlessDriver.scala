package utils

import java.io.{FileOutputStream, File}

import org.openqa.selenium.{OutputType, WebDriver}
import org.openqa.selenium.phantomjs.{PhantomJSDriverService, PhantomJSDriver}
import org.openqa.selenium.remote.DesiredCapabilities

object HeadlessDriver {

  def driver = new PhantomJSDriver(config());

  private def config() = {
    val caps =new DesiredCapabilities()
    caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "phantomjs")
    caps.setJavascriptEnabled(true)
    caps.setCapability("takesScreenshot", true)
    caps
  }
}
