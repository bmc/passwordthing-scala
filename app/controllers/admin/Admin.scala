package controllers.admin

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
object Admin extends Controller with Secured with ControllerUtil {
  def index = withAdminUser { currentUser => implicit request =>
    Ok(views.html.admin.index(currentUser))
  }
}
