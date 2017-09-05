package assesementtest

import db.nosql.NoSQL
import org.scalatestplus.play._
import services.ShrinkerImplementation

import scala.collection.mutable

class TestShrinker extends PlaySpec {

  class fakeNosql() extends NoSQL(

  ) {
    val hash = new mutable.HashMap[String, String]()
    override def put(key: String, value: String): Unit = hash.put(key, value)
    override def get(key: String): Option[String] = hash.get(key)
    def clear() = hash.clear()

  }


  class ShrinkerWithMocks(nosql: NoSQL) extends ShrinkerImplementation(nosql) {
    override def getServer(): String = "localhost:8080"

    override def getProtocol(): String = "http"

    override def getThreadPoolSize(): Int = 10

    override def getCurrentThreadPosition(): Int = 2
  }


  "Shrinker getTiny" should {
    "Save Correctly into NOSQL both tiny as key and url as key" in {
      val fakeNoSQL = new fakeNosql()
      val shrinker = new ShrinkerWithMocks(fakeNoSQL)

      val tiny = shrinker.getOrGenerateTiny("www.clarin.com")
      tiny mustBe "http://localhost:8080/c"

      fakeNoSQL.get("tiny::c").get mustBe "www.clarin.com"
      fakeNoSQL.get("url::www.clarin.com").get mustBe "c"
    }


    "get a saved tiny when available" in {
      val fakeNoSQL = new fakeNosql()
      val shrinker = new ShrinkerWithMocks(fakeNoSQL) {
        override def getServer(): String = "localhost:8080"

        override def getProtocol(): String = "http"

        override def getThreadPoolSize(): Int = 10

        override def getCurrentThreadPosition(): Int = 2
      }
      fakeNoSQL.put("url::www.clarin.com", "XX")
      val tiny = shrinker.getOrGenerateTiny("www.clarin.com")
      tiny mustBe "http://localhost:8080/XX"


    }

  }

  "increment counter should " should {
    "increment" in {
      val fakeNoSQL = new fakeNosql()
      val shrinker = new ShrinkerWithMocks(fakeNoSQL){
        override  val SAVE_AFTER_INCREMENTS = 1
      }
      shrinker.incrementAndSaveCounter()

      fakeNoSQL.get("increment.key") mustBe Some("1")

      shrinker.incrementAndSaveCounter()

      fakeNoSQL.get("increment.key") mustBe Some("2")

    }
  }


  "encode should " should {
    "encode correctly" in {
      val fakeNoSQL = new fakeNosql()
      val shrinker = new ShrinkerWithMocks(fakeNoSQL)
      shrinker.encode(1) mustBe "1"
      shrinker.encode(10) mustBe "a"
      shrinker.encode(36) mustBe "A"

    }
  }

  "isThisMyServer " should {
    "match positively and should mismatch when not my server" in {
      val fakeNoSQL = new fakeNosql()
      val shrinker = new ShrinkerWithMocks(fakeNoSQL)

      shrinker.isThisMyServer("http://www.altera.com") mustBe false
      shrinker.isThisMyServer("http://localhost:8080/steaks") mustBe true
      shrinker.isThisMyServer("http://localhost:3030/steaks") mustBe false

    }
  }
  "isThisMyScheme " should {
    "match positively and should mismatch when not my server" in {
      val fakeNoSQL = new fakeNosql()
      val shrinker = new ShrinkerWithMocks(fakeNoSQL)

      shrinker.isThisMyScheme("https://www.altera.com") mustBe false
      shrinker.isThisMyScheme("ftp://localhost:8080/steaks") mustBe false
      shrinker.isThisMyScheme("http://localhost:3030/steaks") mustBe true
      shrinker.isThisMyScheme("http://localhost:8080/steaks") mustBe true

    }
  }

  "isThisMyTiny " should {
    "match positively and should mismatch when not my server" in {
      val fakeNoSQL = new fakeNosql()
      val shrinker = new ShrinkerWithMocks(fakeNoSQL)
      fakeNoSQL.put("tiny::XX", "url::www.clarin.com")

      shrinker.isThisMyTiny("http://localhost:8080/XX") mustBe true
      shrinker.isThisMyTiny("http://localhost:8080/XX") mustBe true //its ok we do not check for server or port in this method
      shrinker.isThisMyTiny("http://localhost:8080/DD") mustBe false
    }
  }


  "getUrl " should {
    "return correct URL from tiny" in {
      val fakeNoSQL = new fakeNosql()
      val shrinker = new ShrinkerWithMocks(fakeNoSQL) {
        override def tinyPathToTiny(url: String): String = "zz"
      }
      fakeNoSQL.put("tiny::zz", "www.dpreview.com")

      shrinker.getUrl("http://localhost:8080/zz") mustBe "http://www.dpreview.com"
    }
  }

  "tinyPathToTiny " should {
    "clean path slashes from relative path from uri" in {
      val fakeNoSQL = new fakeNosql()
      val shrinker = new ShrinkerWithMocks(fakeNoSQL)

      shrinker.tinyPathToTiny("http://localhost/xxx/") mustBe "xxx"
      shrinker.tinyPathToTiny("http://localhost/xxx") mustBe "xxx"


    }
  }

  "calculateUrl " should {
    "should know if its own tiny or external url" in {
      val fakeNoSQL = new fakeNosql()
      val shrinkerPositiveTest = new ShrinkerWithMocks(fakeNoSQL) {
        override def isThisMyServer(url: String): Boolean = true
        override def verifyTinyUrl(urlParameter: String): Unit = Unit
        override def getOrGenerateTiny(url: String): String = "http://localhost:8080/xxxx"
      }

      shrinkerPositiveTest.calculateUrl("http://localhost/xxx/") mustBe "http://localhost/xxx/"
      val shrinkerNegativeTest = new ShrinkerWithMocks(fakeNoSQL) {
        override def isThisMyServer(url: String): Boolean = false
        override def verifyTinyUrl(urlParameter: String): Unit = Unit
        override def getOrGenerateTiny(url: String): String = "http://localhost:8080/xxxx"
      }

      shrinkerNegativeTest.calculateUrl("http://google.com/xxx/") mustBe "http://localhost:8080/xxxx"

    }

    "verifyTinyUrl " should {
      "should not barf if its own scheme and tiny is in db" in {
        val fakeNoSQL = new fakeNosql()
        val shrinker = new ShrinkerWithMocks(fakeNoSQL) {
          override def isThisMyScheme(url: String): Boolean = true

          override def isThisMyTiny(url: String): Boolean = true
        }

        try {
          shrinker.verifyTinyUrl("whatever")
          true mustBe true
        } catch {
          case _ => true mustBe false
        }
      }

      "should  barf if not own scheme" in {
        val fakeNoSQL = new fakeNosql()
        val shrinker = new ShrinkerWithMocks(fakeNoSQL) {
          override def isThisMyScheme(url: String): Boolean = false

          override def isThisMyTiny(url: String): Boolean = true
        }

        try {
          shrinker.verifyTinyUrl("whatever")
          true mustBe false
        } catch {
          case _:Throwable => true mustBe true
        }
      }

      "should  barf if not own tiny" in {
        val fakeNoSQL = new fakeNosql()
        val shrinker = new ShrinkerWithMocks(fakeNoSQL) {
          override def isThisMyScheme(url: String): Boolean = true

          override def isThisMyTiny(url: String): Boolean = false
        }

        try {
          shrinker.verifyTinyUrl("whatever")
          true mustBe false
        } catch {
          case _ => true mustBe true
        }
      }

    }
  }


}
