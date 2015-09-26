package grinder.test

import java.util.concurrent.TimeUnit

import org.openqa.selenium.WebDriver
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSpec
import org.scalatest.{BeforeAndAfterAll, FunSpec}
import org.scalatest.selenium.WebBrowser


trait Driver  extends FunSpec with BeforeAndAfterAll with WebBrowser {
  implicit def  driver: WebDriver

  def setTimeout() {driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS)}

  override protected def afterAll = driver.close()
}
