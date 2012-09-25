package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.json.Json

import models.User._
import models._

import controllers.util._

object Application extends Controller with Secured with ControllerUtil {

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
        ObjectNotFound(routes.Application.index())

      case Right(site) if site.userID != currentUser.id.get =>
        ObjectAccessDenied(routes.Application.index())

      case Right(site) =>
        Ok(views.html.site(currentUser, site))
    }
  }

  private val editSiteForm = Form(
    mapping(
      "name"         -> nonEmptyText,
      "username"     -> optional(text),
      "email"        -> optional(text),
      "password"     -> optional(text),
      "url"          -> optional(text),
      "notes"        -> optional(text),
      "userID"       -> longNumber
    )
    (applyForEdit)(unapplyForEdit)
  )

  def editSite(id: Long) = ActionWithUser { currentUser => implicit request =>
    Site.findByID(id) match {
      case Left(error) =>
        ObjectNotFound(routes.Application.index())

      case Right(site) if (site.userID != currentUser.id.get) =>
        ObjectAccessDenied(routes.Application.index())

      case Right(site) =>
        Ok(views.html.editsite(site, currentUser, editSiteForm.fill(site)))
    }
  }

  def updateSite(id: Long) = {
    ActionWithUser(parse.urlFormEncoded) {
      currentUser => implicit request =>

      editSiteForm.bindFromRequest.fold (

        // Failure. Repost.
        { form =>
          Site.findByID(id) match {
            case Left(error) =>
              Redirect(routes.Application.index()).
                flashing("error" -> ("Can't find site with ID " + id))

            case Right(site) =>
              BadRequest(views.html.editsite(site, currentUser, form))
          }
        },

        { site =>

          // The ID isn't part of the form-built site. Use the case-class copy()
          // functionality to copy one into place.
          Site.update(site.copy(id = Some(id))) match {
            case Left(error) =>
              val filledForm = editSiteForm.fill(site)
              // Can't use "flashing" here, because the template will already
              // have been rendered by the time Ok is called. Instead, create
              // our own flash object and pass it to the template.
              //
              // This COULD be done with an implicit parameter, but using an
              // implicit parameter leads to less obvious code. Here's how,
              // though:
              //
              //     implicit val flash = Flash(Map(...))
              //     Ok(views.html.admin.edituser(...))
              val flash = Flash(Map("error" -> error))
              Ok(views.html.editsite(site, currentUser, filledForm)(flash))

            case Right(worked:Boolean) =>
              Redirect(routes.Application.editSite(id)).
                flashing("info" -> "Saved.")
          }
        }
      )
    }
  }

  def newSite = myToDo()

  def createSite = myToDo()

  def deleteSite(id: Long) = myToDo()

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------


  private def AccessDenied = {
    Results.Redirect(routes.Application.index()).
      flashing("error" -> "Access denied.")
  }

  private def applyForEdit(name:     String,
                           username: Option[String],
                           email:    Option[String],
                           password: Option[String],
                           url:      Option[String],
                           notes:    Option[String],
                           userID:   Long) = {
    Site(name, username, email, password, url, notes, userID)
  }

  private def unapplyForEdit(site: Site) = {
    Some((site.name,
          site.username,
          site.email,
          site.password,
          site.url,
          site.notes,
          site.userID))
  }

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
