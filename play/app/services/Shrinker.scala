package services

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject._
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.model.Uri
import db.nosql._

trait Shrinker {
  def calculateUrl(url: String): String

  def getUrl(tiny: String): String
}


@Singleton
class ShrinkerImplementation @Inject()(nosql: NoSQL) extends Shrinker {

  val SAVE_AFTER_INCREMENTS = 200

  val incrementor: AtomicLong = new AtomicLong()
  val server = getServer()

  protected def getServer(): String = {
    ConfigFactory.load().getString("application.server") + ":" + ConfigFactory.load().getString("http.port")
  }

  val protocol = getProtocol()

  protected def getProtocol() = {
    ConfigFactory.load().getString("application.protocol")
  }

  private val incrementKey = nosql.get("increment.key") match {
    case Some(e: String) => e.toLong + SAVE_AFTER_INCREMENTS
    case _ => {
      nosql.put("increment.key", "0")
      0L
    }
  }
  incrementor.set(incrementKey)


  /**
    * retrieves tiny if in db or generates and retrieves if it does not exist
    * @param url
    * @return
    */
  def getOrGenerateTiny(url: String): String = {
    nosql.get("url::" + url) match {
      case Some(e: String) => protocol + "://" + server + "/" + e
      case None => {

        val numericKey =  incrementAndSaveCounter()
        val key = encode(numericKey)
        nosql.put("tiny::" + key, url)
        nosql.put("url::" + url, key)
        protocol + "://" + server + "/" + key
      }
    }
  }

  /**
    * increment the incrementor used to generate the tiny key to be saved
    * @return
    */
  def incrementAndSaveCounter(): Long = {
    this.synchronized {
      val long = incrementor.incrementAndGet()
      if(long % SAVE_AFTER_INCREMENTS == 0) {
        nosql.put("increment.key", long.toString)
      }
      long
    }
  }

  val symbols = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

  /**
    * encode a long number into a more compressed word
    * @param number
    * @return
    */
  def encode(number: Long): String = {
    var num = number
    val B = symbols.length
    val sb = new StringBuilder
    while ( {
      num != 0
    }) {
      sb.append(symbols.charAt((num % B).toInt))
      num = num / B
    }
    sb.reverse.toString
  }


  /**
    * check if URI points to own server
    * @param url
    * @return
    */
  def isThisMyServer(url: String): Boolean = {
    val uri = new java.net.URI(url.trim)
    uri.getAuthority == server
  }


  /**
    * check if uri shares the same scheme/protocol as they server
    * @param url
    * @return
    */
  def isThisMyScheme(url: String): Boolean = {
    val uri = new java.net.URI(url)
    val scheme = uri.getScheme match {
      case e:String => e
      case _ => "http"
    }
    scheme == protocol
  }

  /**
    * check if tiny is actually a tiny key stored in db
    * @param url
    * @return
    */
  def isThisMyTiny(url: String): Boolean = {
    try {
      val tiny = tinyPathToTiny(url)
      nosql.get("tiny::" + tiny) match {
        case Some(e: String) => true
        case None => false

      }
    } catch {
      case e: Exception => false
    }
  }


  /**
    * get url assumming argument is tiny address
    * @param url
    * @return
    */
  def getUrl(url: String): String = {
    val tiny = tinyPathToTiny(url)
    nosql.get("tiny::" + tiny) match {
      case Some(e: String) => {
        if (new java.net.URI(e).getScheme == null) {
          "http://" + e
        } else {
          e
        }
      }
    }
  }

  /**
    * Calculate tiny from path even if it has slashes
    * @param url
    * @return
    */
  def tinyPathToTiny(url: String) = {

    val tinyPath: String = new java.net.URI(url).getPath
    val exp = """^/{0,1}([a-zA-Z0-9_]*)/{0,1}""".r
    val exp(tiny) = tinyPath
    tiny
  }

  /**
    * gets index position of current thread relative from thread pool list
    * @return
    */
  protected def getCurrentThreadPosition() = {
    val threadSet = Thread.getAllStackTraces.keySet
    val threadArray = threadSet.toArray(new Array[Thread](threadSet.size))
    threadArray.indexOf(Thread.currentThread())
  }

  /**
    * get the number of threads in the thread pool run by play
    * @return
    */
  protected def getThreadPoolSize(): Int = {
    Thread.getAllStackTraces.keySet.size()
  }


  /**
    * this function returns own address if parameter matches server name or returns a tiny if argument is external address
    * @param urlParameter
    * @return
    */
  def calculateUrl(urlParameter: String): String = {
    if (isThisMyServer(urlParameter)) {
      verifyTinyUrl(urlParameter)
      urlParameter
    } else {
      getOrGenerateTiny(urlParameter)
    }
  }


  /**
    * verify tiny url shares same protocol in address and is real.
    * By now we already checked if address is own
    * @param urlParameter
    */
  def verifyTinyUrl(urlParameter: String) = {
    if (!isThisMyScheme(urlParameter)) {
      throw new Error("Error: invalid path: " + urlParameter)
    }
    if (!isThisMyTiny(urlParameter)) {
      throw new Error("Error: following tiny does not exist: " + urlParameter)
    }
  }
}
