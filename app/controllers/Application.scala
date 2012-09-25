package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form

import models.User._
import models._

import controllers.util._

object Application extends Controller with Secured {
  
  def index = ActionWithUser { user => implicit request =>
    Ok(views.html.index(user))
  }

  def listSites = ActionWithUser { user => implicit request =>
    Redirect(routes.Application.index())
  }

  def editSite(id: Long) = myToDo()

  def updateSite(id: Long) = myToDo()

  def newSite = myToDo()

  def createSite = myToDo()

  def myToDo(message: String = "") = {
    ActionWithUser { user => implicit request =>
      Ok(views.html.mytodo(message, user))
    }
  }
}
