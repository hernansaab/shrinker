package controllers

import java.util
import javax.inject._

import play.api.cache.redis.CacheApi
import play.api.mvc._
import scala.util.Success
import scala.util.Failure
import scala.util.Try
@Singleton
class RouterController @Inject()(cc: ControllerComponents, shrinker:services.Shrinker) extends AbstractController(cc) {

  def route(id:String) = Action {
    Try(shrinker.getUrl(id)) match {
      case Success(url)   => Ok(views.html.redirect(Map("url" -> url)))
      case Failure(error) => Ok(views.html.error())
    }

  }
}
