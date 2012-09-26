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
    Site.count(currentUser).fold(
      { error =>

        val flash = Flash(Map("error" -> error))
        Ok(views.html.sites.index(currentUser)(flash))
      },

      { count =>

        Ok(views.html.sites.index(currentUser, Some(count)))
      }
    )
  }

  def list = ActionWithUser { currentUser => implicit request =>
    Site.allForUser(currentUser).fold(
      error => Ok(sitesJson(Nil, Some(error))),
      sites => Ok(sitesJson(sites))
    )
  }

  def show(id: Long) = ActionWithUser { currentUser => implicit request =>
    Site.findByID(id, currentUser)fold(
      error => ObjectNotFound(routes.SiteController.index()),
      site  => Ok(views.html.sites.show(currentUser, site))
    )
  }

  def showJSON(id: Long) = ActionWithUser { currentUser => implicit request =>
    val res = Site.findByID(id, currentUser).fold(
      error => Map("error" -> Json.toJson("Not found")),
      site  => Map("site" -> site.toJson)
    )

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
    Site.findByID(id, currentUser).fold(
      { error =>

        ObjectNotFound(routes.SiteController.index())
      },

      { site =>

        Ok(views.html.sites.edit(site.id.get,
                                 Some(site.name),
                                 currentUser,
                                 siteForm.fill(site)))

      }
    )
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
          Site.update(site.copy(id = Some(id))).fold(
            { error =>

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
              val filledForm = siteForm.fill(site)
              Ok(views.html.sites.edit(id, Some(site.name), currentUser, filledForm)
                                      (flash))
            },

            { _ =>

              Redirect(routes.SiteController.edit(id)).
                flashing("info" -> "Saved.")
            }
          )
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

          Site.create(site, currentUser).fold(
            { error =>

              val filledForm = siteForm.fill(site)
              val flash = Flash(Map("error" -> error))
              Ok(views.html.sites.makeNew(currentUser, filledForm)(flash))
            },

            { dbSite =>

              Redirect(routes.SiteController.edit(dbSite.id.get)).
                flashing("info" -> "Saved.")
            }
          )
        }
      )
    }
  }

  def delete(id: Long) = ActionWithUser { currentUser => implicit request =>
    val error = Site.delete(id).fold(
      error => Some(error),
      worked => None
    )

    Site.allForUser(currentUser).fold(
      { error2 =>

        // Combine the errors, assuming there's a first one.
        val e = error.map(_ + ", " + error2).getOrElse(error2)
        Ok(sitesJson(Nil, Some(e)))
      },

      {
        sites =>

        Ok(sitesJson(sites, error))
      }
    )
  }

  def search(q: String) = ActionWithUser { currentUser => implicit request =>
    Site.search(q, currentUser).fold(
      { error =>

        Logger.error("Search failed for user=\"" + currentUser.username +
                     "\", q=\"" + q + "\": " + error)
        Ok(Json.toJson(Array.empty[String]))
      },

      {
        sites =>

        // Must send back an array of {"id": "n", "name": "___"} elements
        val map = sites.map {
          s => Map("id" -> s.id.get.toString, "name" -> s.name, "readonly" -> "true")
        }
        Ok(Json.toJson(map))
      }
    )
  }

  def download = ActionWithUser { currentUser => implicit request =>
    import au.com.bytecode.opencsv.CSVWriter
    import java.io.StringWriter

    Site.allForUser(currentUser).fold(

      { error =>

        Redirect(routes.SiteController.index()).
          flashing("error" -> ("Can't get your sites: " + error))
      },

      { sites =>

        val buf = new StringWriter
        val csv = new CSVWriter(buf, ',')
        csv.writeNext(
          Array("name", "username", "email", "password", "url", "notes")
        )

        sites.foreach { site =>
          csv.writeNext(
            Array(
              site.name,
              site.username.getOrElse(""),
              site.email.getOrElse(""),
              site.password.getOrElse(""),
              site.url.getOrElse(""),
              site.notes.getOrElse("")
            )
          )
        }

        csv.close()
        Ok(buf.toString).
          as("text/csv").
          withHeaders(CONTENT_DISPOSITION -> "attachment; filename=sites.csv")
      }
    )
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
