package controllers.util

import play.api._
import play.api.mvc._

import models.User._
import models._

trait ControllerUtil {
  self: Controller with Secured =>

  def ObjectNotFound(redirectTo: Call)(implicit request: RequestHeader) = {
    Results.Redirect(redirectTo).flashing("error" -> "Not found.")
  }

  def ObjectAccessDenied(redirectTo: Call)(implicit request: RequestHeader) = {
    Results.Redirect(redirectTo).flashing("error" -> "Access denied.")
  }
}