package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form

import models.User._
import models._

object Admin extends Controller with Secured with ControllerUtil {
  
  def index = withAdminUser { user => implicit request =>
    Ok(views.html.admin.index(user))
  }

  def listUsers = withAdminUser { user => implicit request =>
    Ok(views.html.admin.index(user))
  }
}
