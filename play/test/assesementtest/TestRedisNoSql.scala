package assesementtest

import controllers.HomeController
import db.nosql.RedisNoSQL
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.cache.redis.CacheApi
import services.Shrinker
import org.mockito.Mockito._

class TestRedisNoSql  extends PlaySpec  with MockitoSugar{


  "Controller" should {
    "display a good url if succesful generation of url" in {
      val mockCacheApi = mock[CacheApi]
      when(mockCacheApi.get[String]("url::www.clarin.com")) thenReturn Some("tiny::cccc")
      when(mockCacheApi.set("url::www.clarin.com", "xxx")) thenReturn null

      val redis = new RedisNoSQL(mockCacheApi)
      redis.put("url::www.clarin.com", "xxx")

      redis.get("url::www.clarin.com") mustBe Some("tiny::cccc")

    }





  }
}
