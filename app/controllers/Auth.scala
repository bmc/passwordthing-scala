package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.Crypto

import models.User._
import models._

import controllers._
import controllers.util._

object Auth extends Controller {

  // ----------------------------------------------------------------------
  // Actions
  // ----------------------------------------------------------------------

  val loginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )
    (applyForLogin)
    (unapplyForLogin)
    verifying("Invalid user name and password", validLogin _)
  )

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def logout = Action {
    Redirect(routes.Auth.login()).withNewSession
  }

  def authenticate = Action(parse.urlFormEncoded) { implicit request =>

    // "Folding" the form means providing two values. The first value is
    // returned if the form failed validation. The second value is returned
    // if the form passes validation.
    loginForm.bindFromRequest.fold(

      // Failure: Re-post the login form.
      { form =>

        BadRequest(views.html.login(form))
      },

      // Success. Redirect to the main application.
      { user => 

        val fullUser = User.findByUsername(user.username).fold(
          error  => user,
          dbUser => dbUser
        )

        Redirect(routes.Application.index()).
          withSession("username" -> user.username).
          flashing("info" -> ("Welcome back, " + fullUser.displayName))
      }
    )
  }

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  private def applyForLogin(name: String, password: String): User =
    User(name, encrypt(password), None, None, None, None, None, false)

  private def unapplyForLogin(user: User) =
    Some((user.username, user.encryptedPassword))

  private def validLogin(u: User): Boolean = {
    User.findByUsername(u.username).fold(
      { error =>

        Logger.error(
          "Invalid login for user %s: %s".format(u.username, error)
        )
        false
      },

      { dbUser =>

        val valid = dbUser.encryptedPassword == u.encryptedPassword
        if (!valid) Logger.error("Bad password for user %s.".format(u.username))
        valid
      }
    )
  }
}