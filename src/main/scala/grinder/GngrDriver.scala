/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2015 Uproot Labs India Pvt Ltd

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

 */

package grinder

import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.TimeUnit
import org.openqa.selenium.Cookie
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriver.ImeHandler
import org.openqa.selenium.WebDriver.Navigation
import org.openqa.selenium.WebDriver.Options
import org.openqa.selenium.WebDriver.Timeouts
import org.openqa.selenium.WebDriver.Window
import org.openqa.selenium.logging.Logs
import java.io.InputStream
import java.io.ObjectInputStream
import java.awt.image.Raster
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.MessageDigest
import java.util.Base64
import org.openqa.selenium.Dimension

class GngrDriver(authKey: String) extends WebDriver with TakesScreenshot {
  private val gngrWindow = new Window {
    def getPosition(): org.openqa.selenium.Point = ???
    def getSize(): org.openqa.selenium.Dimension = ???
    def maximize() {

    }

    def setPosition(x$1: org.openqa.selenium.Point): Unit = ???
    def setSize(dim: Dimension) {
      execute(grinderPort, s"SET_SIZE ${dim.getWidth} ${dim.getHeight}")
    }
  }

  private val gngrTimeouts = new Timeouts {
    def implicitlyWait(x$1: Long, x$2: TimeUnit): Timeouts = this
    def pageLoadTimeout(x$1: Long, x$2: TimeUnit): Timeouts = this
    def setScriptTimeout(x$1: Long, x$2: TimeUnit): Timeouts = this
  }

  private val options = new Options {

    def addCookie(x$1: Cookie): Unit = ???
    def deleteAllCookies(): Unit = ???
    def deleteCookie(x$1: Cookie): Unit = ???
    def deleteCookieNamed(x$1: String): Unit = ???
    def getCookieNamed(x$1: String): Cookie = ???
    def getCookies(): java.util.Set[Cookie] = ???
    def ime(): ImeHandler = ???
    def logs(): Logs = ???
    def timeouts(): Timeouts = gngrTimeouts
    def window(): Window = gngrWindow
  }

  private val grinderPort = getGrinderPort

  private def getGngrPort = {
    val userHome = System.getProperty("user.home")
    val appHome = new File(userHome, ".gngr")
    val profileHome = new File(appHome, "default")
    val portFile = new File(profileHome, "port.dat")
    if (!portFile.exists()) {
      throw new InvalidConfigurationException("Port file not found. `gngr` is probably not running")
    }

    val in = new FileInputStream(portFile)
    val din = new DataInputStream(in)
    din.readInt()
  }

  private def getGrinderPort: Int = {
    execute(getGngrPort, (writer, is) => {
      val digest = MessageDigest.getInstance("SHA-256");
      val authKeyHash = digest.digest(authKey.getBytes("UTF-8"));
      val authKeyHashB64 = Base64.getEncoder.encodeToString(authKeyHash)
      writer.write(s"GRINDER $authKeyHashB64\r\n")
      writer.flush()
      val dis = new DataInputStream(is)
      val gsPort = dis.readInt()
      sendAck(writer)
      if (gsPort < 0) {
        throw new InvalidConfigurationException("Auth key rejected")
      }
      gsPort
    })
  }

  private def getSocket(port: Int) = {
    val bindHost = InetAddress.getByAddress(Array[Byte](127, 0, 0, 1))
    val s = new Socket(bindHost, port)
    s.setTcpNoDelay(true)
    s
  }

  private def execute(port: Int, command: String, ack: Boolean = true) {
    // println(s"Executing $command on $port")
    execute(port, (writer, is) => {
      writer.write(command + "\r\n")
      writer.flush()

      if (ack) {
        getConfirmationAndSendAck(is, writer)
      }
    })
  }

  private def execute[T](port: Int, responseHandler: (OutputStreamWriter, InputStream) => T): T = {
    val socket = getSocket(port)
    val out = socket.getOutputStream()
    val is = socket.getInputStream
    val writer = new OutputStreamWriter(out)

    responseHandler(writer, is)
  }

  def close() {
    execute(grinderPort, "CLOSE", ack = false)
  }

  def findElement(x$1: org.openqa.selenium.By): org.openqa.selenium.WebElement = ???
  def findElements(x$1: org.openqa.selenium.By): java.util.List[org.openqa.selenium.WebElement] = ???
  def get(x$1: String): Unit = ???
  def getCurrentUrl(): String = ???
  def getPageSource(): String = ???
  def getTitle(): String = ???
  def getWindowHandle(): String = ???
  def getWindowHandles(): java.util.Set[String] = ???
  def manage(): Options = {
    options
  }

  private val gngrNavigation = new Navigation {
    def back(): Unit = ???
    def forward(): Unit = ???
    def refresh(): Unit = ???
    def to(u: URL): Unit = {
      to(u.toExternalForm())
    }
    def to(us: String): Unit = {
      execute(grinderPort, (writer, is) => {
        writer.write("TO " + us + "\r\n")
        writer.flush()
        getConfirmationAndSendAck(is, writer)
      })
    }

  }

  def navigate(): Navigation = gngrNavigation

  def quit(): Unit = {
    // TODO: Quit should also delete the profile (when profiles are implemented)
    execute(grinderPort, "QUIT", ack = false)
  }

  def switchTo(): org.openqa.selenium.WebDriver.TargetLocator = ???

  def getScreenshotAs[T](ot: OutputType[T]): T = {
    ot match {
      case OutputType.BYTES =>
        val bytes = execute(grinderPort, (writer, is) => {
          writer.write("SCREENSHOT\r\n")
          writer.flush()
          val fileSize = (new DataInputStream(is)).readInt()
          val bytes = readInputStream(is, fileSize)
          sendAck(writer)

          bytes
        })
        ot.convertFromPngBytes(bytes)
    }
  }

  private def sendAck(writer: java.io.OutputStreamWriter) = {
    writer.write("ACK\r\n")
    writer.flush()
  }

  private def getConfirmationAndSendAck(is: java.io.InputStream, writer: java.io.OutputStreamWriter) = {
    (new DataInputStream(is)).readInt()
    sendAck(writer)
  }

  private def readInputStream(is: InputStream, maxSize: Int) = {
    val buffer = new ByteArrayOutputStream()

    var nRead = 0
    var countRead = 0
    val data = new Array[Byte](16384)

    while (countRead < maxSize && nRead != -1) {
      nRead = is.read(data, 0, data.length)
      if (nRead != -1) {
        countRead += nRead
        buffer.write(data, 0, nRead);
      }
    }

    buffer.flush()
    buffer.toByteArray()
  }
}
