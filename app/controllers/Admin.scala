package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.json.Json

import models.User._
import models._

// Not the real user. Only used internally.

object Admin extends Controller with Secured with ControllerUtil {

  val editUserForm = Form(
    mapping(
      "username"              -> nonEmptyText(minLength=3).
                                 verifying("User already exists", uniqueUser _),
      "password"              -> text.verifying(validPassword _),
      "password_confirmation" -> text.verifying(validPassword _),
      "isAdmin"               -> checked("Admin")
    )
    (User.applyForEdit)
    (User.unapplyForEdit)
    verifying("Passwords don't match.", { user =>
      user.password == user.passwordConfirmation
    })
  )

  def index = withAdminUser { user => implicit request =>
    Ok(views.html.admin.index(user, editUserForm))
  }

  def listUsers = withAdminUser { user => implicit request =>
    Ok(Json.toJson(User.all.map {_.toJson}))
  }

  // A valid password must have at least one number, one or more characters,
  // at at least one punctuation character. Length is asserted elsewhere.
  private val ValidPassword = """^(?=.*\d)(?=.*[a-zA-Z])(?=.*[-!@#$%^&*.,_:;])""".r

  private def validPassword(password: String) = {
    ValidPassword.findFirstIn(password).map {s =>
      (s.length >= 8) && (s.length <= 32)
    }.getOrElse(false)
  }

  private def uniqueUser(username: String) = {
    User.findByName(username) match {
      case Left(error) => true
      case Right(user) => false
    }
  }
}
