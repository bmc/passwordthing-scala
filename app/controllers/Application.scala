package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.json.Json

import models.User._
import models._

import controllers.util._

object Application extends Controller with Secured {

  // ----------------------------------------------------------------------
  // Actions
  // ----------------------------------------------------------------------

  def index = ActionWithUser { currentUser => implicit request =>
    Site.count(currentUser) match {
      case Left(error) =>
        val flash = Flash(Map("error" -> error))
        Ok(views.html.index(currentUser)(flash))

      case Right(count) =>
        Ok(views.html.index(currentUser, Some(count)))
    }
  }

  def listSites = ActionWithUser { currentUser => implicit request =>
    Site.allForUser(currentUser) match {
      case Left(error)  => Ok(siteJson(Nil, Some(error)))
      case Right(sites) => Ok(siteJson(sites))
    }
  }

  def showSite(id: Long) = ActionWithUser { currentUser => implicit request =>
    Site.findByID(id) match {
      case Left(error) =>
        Redirect(routes.Application.index()).flashing("error" -> "Site not found")

      case Right(site) if site.userID != currentUser.id.get =>
        Redirect(routes.Application.index()).flashing("error" -> "Access denied")

      case Right(site) =>
        Ok(views.html.site(currentUser, site))
    }
  }

  def editSite(id: Long) = myToDo()

  def updateSite(id: Long) = myToDo()

  def newSite = myToDo()

  def createSite = myToDo()

  def deleteSite(id: Long) = myToDo()

  def myToDo(message: String = "") = {
    ActionWithUser { currentUser => implicit request =>
      Ok(views.html.mytodo(message, currentUser))
    }
  }

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  private def siteJson(sites: Seq[Site], errorMessage: Option[String] = None) = {
    val sitesMap = Json.toJson(sites.map {_.toJson})
    Json.stringify(
      Json.toJson(
        errorMessage.map(e =>
          Map("error" -> Json.toJson(e), "sites" -> sitesMap)
        ).getOrElse(
          Map("sites" -> sitesMap)
        )
      )
    )
  }
}
