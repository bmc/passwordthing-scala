package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.json.Json

import models.User._
import models._

import controllers.util._

object Application extends Controller with Secured with ControllerUtil {

  // ----------------------------------------------------------------------
  // Actions
  // ----------------------------------------------------------------------

  def index = ActionWithUser { currentUser => implicit request =>
    Redirect(routes.SiteController.index())
  }
}
