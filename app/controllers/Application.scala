package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form

import models.User._
import models._

import controllers.util._

object Application extends Controller with Secured {
  
  def index = withUser { user => implicit request =>
    Ok(views.html.index(user))
  }

  def sites = myToDo()

  def newSite = myToDo()

  def myToDo(message: String = "") = withUser { user => implicit request =>
    Ok(views.html.mytodo(message, user))
  }
}
