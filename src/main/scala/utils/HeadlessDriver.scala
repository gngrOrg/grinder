package utils

import java.io.{FileOutputStream, File}

import org.openqa.selenium.{OutputType, WebDriver}
import org.openqa.selenium.phantomjs.{PhantomJSDriverService, PhantomJSDriver}
import org.openqa.selenium.remote.DesiredCapabilities

object HeadlessDriver{

  val service = new PhantomJSDriverService.Builder()
    .usingPhantomJSExecutable(new File("/Users/sayy05/Downloads/phantomjs-2.0.0-macosx/bin/phantomjs"))
    .usingPort(4444)
    .build();

  val driver = new PhantomJSDriver(service, new DesiredCapabilities(config()));


  private def config() = {
    val caps =new DesiredCapabilities()
    caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "")
    caps.setJavascriptEnabled(true)
    caps.setCapability("takesScreenshot", true)
    caps
  }

//  private def takeScreenShot(name: String) {
//    val bytes = driver.getScreenshotAs(OutputType.BYTES)
//    val fileName = "/Users/sayy05/Desktop/" + name + ".png"
//    val fos = new FileOutputStream(fileName)
//    try {
//      fos.write(bytes)
//    } finally {
//      fos.close()
//    }
//  }

//  driver.navigate().to("http://localhost:8000/nightly-unstable/xhtml1/abs-pos-non-replaced-vlr-213.xht")
//  takeScreenShot("abs-pos-non-replaced-vlr-213")

}
