package controllers

import play.api._
import play.api.mvc._

import models.User._
import models._

// Based on this example: 
// http://www.playframework.org/documentation/2.0.1/ScalaSecurity
trait Secured {
  self: Controller =>

  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) =
    Results.Redirect(routes.Auth.login())

  // Chain this to a controller action, e.g.:
  //
  //    def index = withAuth { username => implicit request =>
  //      Ok(views.html.index(...))
  //    }
  def withAuth(f: String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { username =>
      Action(request => f(username)(request))
    }
  }

  // Chain this to a controller action, e.g.:
  //
  //    def index = withUser { user => implicit request =>
  //      Ok(views.html.index(...))
  //    }
  def withUser(f: User => Request[Any] => Result) = withAuth {
    username => implicit request =>

    // Find the user here. For now, we just create one.
    Some(User(username, "foo")).map { user => 
      f(user)(request)
    }.getOrElse(onUnauthorized(request))
  }
}
