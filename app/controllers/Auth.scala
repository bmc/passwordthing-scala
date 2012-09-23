package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form

import models.User._
import models._

object Auth extends Controller {
  val loginForm = Form(
    mapping(
      "username" -> nonEmptyText(minLength=3),
      "password" -> nonEmptyText(minLength=8)
    )
    (User.apply)
    (User.unapply)
    verifying("Invalid user name and password", checkLogin _)
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

  private def checkLogin(u: User): Boolean = true
}