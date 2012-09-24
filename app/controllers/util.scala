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

  def onUnauthorized(request: RequestHeader) = Results.Forbidden

  def onNeedLogin(request: RequestHeader) =
    Results.Redirect(routes.Auth.login())

  // Chain this to a controller action, e.g.:
  //
  //    def index = withAuth { username => implicit request =>
  //      Ok(views.html.index(...))
  //    }
  def withAuth(f: String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onNeedLogin) { username =>
      Action(request => f(username)(request))
    }
  }

  // Version of withAuth() that accepts a BodyParser and passes it through
  // to the generated Action.
  def withAuth[T](bodyParser: BodyParser[T])
                 (f: String => Request[T] => Result) = {
    Security.Authenticated(username, onNeedLogin) { username =>
      Action(bodyParser)(request => f(username)(request))
    }
  }

  // Chain this to a controller action, e.g.:
  //
  //    def index = withUser { user => implicit request =>
  //      Ok(views.html.index(...))
  //    }
  def withUser(f: User => Request[Any] => Result) = withAuth {
    username => implicit request =>

    callBlockWithUser(username, request, f)
  }

  // Version of withUser() that accepts a BodyParser and passes it through
  // to the generated Action.
  def withUser[T](bodyParser: BodyParser[T])
                 (f: User => Request[T] => Result) = withAuth(bodyParser) {
    username => implicit request =>

    callBlockWithUser(username, request, f)
  }

  // Chain this to a controller action, e.g.:
  //
  //    def index = withAdminUser { user => implicit request =>
  //      Ok(views.html.index(...))
  //    }
  def withAdminUser(f: User => Request[Any] => Result) = withUser {
    user => implicit request =>

    callIfAdmin(user, request, f)
  }

  // Version of withAdminUser() that accepts a BodyParser and passes it through
  // to the generated Action.
  def withAdminUser[T](bodyParser: BodyParser[T])
                      (f: User => Request[Any] => Result) = withUser(bodyParser) {
    user => implicit request =>

    callIfAdmin(user, request, f)
  }

  private def callIfAdmin[T](user: User,
                             request: Request[T],
                             f: User => Request[T] => Result) = {
    if (user.isAdmin)
      f(user)(request)
    else {
      Logger.error(
        "Non-admin user %s attempted unauthorized access.".format(user.username)
      )
      onUnauthorized(request)
    }
  }

  private def callBlockWithUser[T](username: String,
                                   request: Request[T],
                                   f: User => Request[T] => Result) = {
    // Map the user name to the user, and pass it to the block.
    User.findByName(username) match {
      case Left(error) =>
        Logger.error(error)
        onUnauthorized(request)
      case Right(user) =>
        f(user)(request)
    }
  }
}

trait ControllerUtil {
  self: Controller with Secured =>

  def myToDo(message: String = "") = withUser { user => implicit request =>
    Ok(views.html.mytodo(message, user))
  }
}