package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.json.Json

import models.User._
import models._

import controllers.util._

object SiteController extends Controller with Secured with ControllerUtil {

  def index = ActionWithUser { currentUser => implicit request =>
    Site.count(currentUser) match {
      case Left(error) =>
        val flash = Flash(Map("error" -> error))
        Ok(views.html.sites.index(currentUser)(flash))

      case Right(count) =>
        Ok(views.html.sites.index(currentUser, Some(count)))
    }
  }

  def list = ActionWithUser { currentUser => implicit request =>
    Site.allForUser(currentUser) match {
      case Left(error)  => Ok(sitesJson(Nil, Some(error)))
      case Right(sites) => Ok(sitesJson(sites))
    }
  }

  def show(id: Long) = ActionWithUser { currentUser => implicit request =>
    Site.findByID(id, currentUser) match {
      case Left(error) =>
        ObjectNotFound(routes.SiteController.index())

      case Right(site) =>
        Ok(views.html.sites.show(currentUser, site))
    }
  }

  def showJSON(id: Long) = ActionWithUser { currentUser => implicit request =>
    val res = Site.findByID(id, currentUser) match {
      case Left(error) =>
        Map("error" -> Json.toJson("Not found"))

      case Right(site) =>
        Map("site" -> site.toJson)
    }

    Ok(Json.toJson(res))
  }

  private val siteForm = Form(
    mapping(
      "name"         -> nonEmptyText,
      "username"     -> optional(text),
      "email"        -> optional(text),
      "password"     -> optional(text),
      "url"          -> optional(text),
      "notes"        -> optional(text),
      "id"           -> optional(longNumber)
    )
    (Site.apply)(Site.unapply)
  )

  def edit(id: Long) = ActionWithUser { currentUser => implicit request =>
    Site.findByID(id, currentUser) match {
      case Left(error) =>
        ObjectNotFound(routes.SiteController.index())

      case Right(site) =>
        Ok(views.html.sites.edit(site.id.get,
                                 Some(site.name),
                                 currentUser,
                                 siteForm.fill(site)))
    }
  }

  def update(id: Long) = {
    ActionWithUser(parse.urlFormEncoded) {
      currentUser => implicit request =>

      siteForm.bindFromRequest.fold (

        // Failure. Repost.
        { form =>

          BadRequest(views.html.sites.edit(id, None, currentUser, form))
        },

        { site =>

          // The ID isn't part of the form-built site. Use the case-class copy()
          // functionality to copy one into place.
          Site.update(site.copy(id = Some(id))) match {
            case Left(error) =>
              val filledForm = siteForm.fill(site)
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
              Ok(views.html.sites.edit(id, Some(site.name), currentUser, filledForm)
                                      (flash))

            case Right(worked:Boolean) =>
              Redirect(routes.SiteController.edit(id)).
                flashing("info" -> "Saved.")
          }
        }
      )
    }
  }

  def makeNew = ActionWithUser { currentUser => implicit request => 
    Ok(views.html.sites.makeNew(currentUser, siteForm))
  }

  def create = {
    ActionWithUser(parse.urlFormEncoded) { currentUser => implicit request =>
      siteForm.bindFromRequest.fold (

        // Failure. Re-post.
        { form =>
  
          BadRequest(views.html.sites.makeNew(currentUser, form))
        },

        { site =>

          Site.create(site, currentUser) match {
            case Left(error) =>
              val filledForm = siteForm.fill(site)
              val flash = Flash(Map("error" -> error))
              Ok(views.html.sites.makeNew(currentUser, filledForm)(flash))

            case Right(dbSite) => {
              Redirect(routes.SiteController.edit(dbSite.id.get)).
                flashing("info" -> "Saved.")
            }
          }
        }
      )
    }
  }

  def delete(id: Long) = ActionWithUser { currentUser => implicit request =>
    val error = Site.delete(id) match {
      case Left(error) => Some(error)
      case Right(bool) => None
    }

    Site.all match {
      case Left(error2) =>
        // Combine the errors, assuming there's a first one.
        val e = error.map(_ + ", " + error2).getOrElse(error2)
        Ok(sitesJson(Nil, Some(e)))

      case Right(sites) =>
        Ok(sitesJson(sites, error))
    }
  }

  def search(q: String) = ActionWithUser { currentUser => implicit request =>
    Site.search(q, currentUser) match {
      case Left(error) =>
        Logger.error("Search failed for user=\"" + currentUser.username +
                     "\", q=\"" + q + "\": " + error)
        Ok(Json.toJson(Array.empty[String]))

      case Right(sites) =>
        // Must send back an array of {"id": "n", "name": "___"} elements
        val map = sites.map {
          s => Map("id" -> s.id.get.toString, "name" -> s.name, "readonly" -> "true")
        }
        Ok(Json.toJson(map))
    }
  }

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  private def sitesJson(sites: Seq[Site], errorMessage: Option[String] = None) = {
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
