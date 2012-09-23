package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.Crypto

import models.User._
import models._

object Auth extends Controller {
  val loginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )
    (User.applyNamePassword)
    (User.unapplyNamePassword)
    verifying("Invalid user name and password", validLogin _)
  )

  def login = Action {
    Ok(views.html.login(loginForm))
  }

  def logout = Action {
    Redirect(routes.Auth.login()).withNewSession
  }

  def authenticate = Action(parse.urlFormEncoded) { implicit request =>

    // "Folding" the form means providing two values. The first value is
    // returned if the form failed validation. The second value is returned
    // if the form passes validation.
    loginForm.bindFromRequest.fold(theForm => 

      // Failure: Re-post the login form.
      BadRequest(views.html.login(theForm)),

      // Success. Redirect to the main application. "c" is the context.
      c => Redirect(routes.Application.index()).
                    withSession("username" -> c.username)
      )
  }

  private def validLogin(u: User): Boolean = {
    User.findByName(u.username) match {
      case Left(error) =>
        Logger.error(
          "Invalid login for user %s: %s".format(u.username, error)
        )
        false
      case Right(dbUser) =>
        val valid = dbUser.encryptedPassword == u.encryptedPassword
        if (!valid) Logger.error("Bad password for user %s.".format(u.username))
        valid
    }
  }
}