package controllers

import java.util
import javax.inject._

import play.api.mvc._
import db.nosql.NoSQL
import akka.http.scaladsl.model.Uri
import scala.util.Success
import scala.util.Failure
import scala.util.Try

@Singleton
class HomeController @Inject()(cc: ControllerComponents, shrinker: services.Shrinker) extends AbstractController(cc) {

  def index(urlInput: String) = Action {
    val urlParameter: String = urlInput.trim
    val renderValues =  urlParameter match {
      case "" =>
        Map("input" -> urlParameter)
      case _:String =>
        renderData(urlParameter)
    }
    Ok(views.html.index(renderValues))
  }


   def renderData(urlParameter: String):Map[String,String] = {
    Try(shrinker.calculateUrl(urlParameter)) match {
      case Success(url) =>
          Map("input" -> urlParameter, "url" -> url)
      case Failure(error) =>
          Map("input" -> urlParameter, "error" -> error.getMessage)
    }
  }
}
