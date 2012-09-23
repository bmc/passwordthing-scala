package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def sites = myToDo()

  def newSite = myToDo()

  def myToDo(message: String = "") = Action {
    Ok(views.html.mytodo(message))
  }
}