package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.json.Json

import models.User._
import models._

import controllers.util._

/** Main admin controller. Each model that can be administered has its own
  * controller.
  */
object AdminController extends Controller with Secured with ControllerUtil {
  def index = ActionWithAdminUser { currentUser => implicit request =>
    val count = User.count.fold(
      { error =>

        Logger.error("Can't get count of total number of users: " + error)
        None
      },

      { count => Some(count) }
    )
    Ok(views.html.users.index(currentUser, count))
  }
}
