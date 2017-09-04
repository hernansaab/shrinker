package assesementtest

import controllers.HomeController
import org.scalatestplus.play._
import services.Shrinker

class TestHomeController  extends PlaySpec {


  "Controller" should {
    "display a good url if succesful generation of url" in {
      class FakeShrinker extends  Shrinker {
        override def calculateUrl(url: String): String = "whatever2"

        override def getUrl(tiny: String): String = "whatever1"
      }
      val controller = new HomeController(null, new FakeShrinker )

      controller.renderData("whateverX") mustBe( Map("input" -> "whateverX", "url" -> "whatever2"))

    }

    "display an error message if calculateUrl barfs" in {
      class FakeShrinker extends  Shrinker {
        override def calculateUrl(url: String): String = throw new Error("big doodoo")

        override def getUrl(tiny: String): String = "whatever1"
      }
      val controller = new HomeController(null, new FakeShrinker )

      controller.renderData("whateverX") mustBe( Map("input" -> "whateverX", "error" -> "big doodoo"))

    }

  }
}
