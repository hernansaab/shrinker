package controllers

import java.util
import javax.inject._

import play.api.mvc._
import db.nosql.NoSQL
import akka.http.scaladsl.model.Uri

@Singleton
class ErrorController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def error = Action {

    Ok(views.html.error())
  }
}
