package controllers.util

import play.api._
import play.api.mvc._

import models.User._
import models._

trait ControllerUtil {
  self: Controller with Secured =>

  def myToDo(message: String = "") = {
    ActionWithUser { user => implicit request =>
      Ok(views.html.mytodo(message, user))
    }
  }
}