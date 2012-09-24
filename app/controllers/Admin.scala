package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.json.Json

import models.User._
import models._

// Not the real user. Only used internally.

object Admin extends Controller with Secured with ControllerUtil {

  val MinUsernameLength = 3
  val MinPasswordLength = 8
  val MaxPasswordLength = 32
  val PasswordError = 
    """|Passwords must be between %d and %d characters and must have at
       |least one letter, one number, and one punctuation character.""".
    stripMargin.format(MinPasswordLength, MaxPasswordLength)


  val editUserForm = Form(
    mapping(
      "username"              -> nonEmptyText(minLength=MinUsernameLength),
      "password"              -> optional(text.verifying(PasswordError, validPassword _)),
      "password_confirmation" -> optional(text.verifying(PasswordError, validPassword _)),
      "isAdmin"               -> boolean,
      "id"                    -> longNumber
    )
    (User.applyForEdit)
    (User.unapplyForEdit)
    verifying("Passwords don't match.", { user =>
      user.password.getOrElse("") == user.passwordConfirmation.getOrElse("")
    })
  )

  val newUserForm = Form(
    mapping(
      "username"              -> nonEmptyText(minLength=MinUsernameLength).
                                 verifying("User already exists", uniqueUser _),
      "password"              -> text.verifying(PasswordError, validPassword _),
      "password_confirmation" -> text.verifying(PasswordError, validPassword _),
      "isAdmin"               -> boolean
    )
    (User.applyForCreate)
    (User.unapplyForCreate)
    verifying("Passwords don't match.", { user =>
      user.password == user.passwordConfirmation
    })
  )

  def index = withAdminUser { user => implicit request =>
    Ok(views.html.admin.index(user))
  }

  def listUsers = withAdminUser { user => implicit request =>
    Ok(Json.toJson(User.all.map {_.toJson}))
  }

  def editUser(id: Long) = withAdminUser { currentUser => implicit request =>
    User.findByID(id) match {
      case Left(error) =>
        Redirect(routes.Admin.index()).flashing("error" -> error)
      case Right(user) =>
        val filledForm = editUserForm.fill(user)
        Ok(views.html.admin.edituser(user, currentUser, filledForm))
    }
  }

  def saveUser = withAdminUser(parse.urlFormEncoded) {
    currentUser => implicit request =>

    editUserForm.bindFromRequest.fold (

      // Failure. Re-post.
      { form =>

        val id = form("id").value.get.toInt
        User.findByID(id) match {
          case Left(error) =>
            Redirect(routes.Admin.index).
              flashing("error" -> ("Unable to find user with ID " + id.toString))

          case Right(user) =>
            BadRequest(views.html.admin.edituser(user, currentUser, form)).
              flashing("error" -> "Validation failed.")
        }
      },
        

      { user =>
        User.update(user) match {
          case Left(error) =>
            val filledForm = editUserForm.fill(user)
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
            Ok(views.html.admin.edituser(user, currentUser, filledForm)(flash))

          case Right(worked:Boolean) =>
            Redirect(routes.Admin.editUser(user.id.get)).
              flashing("info" -> "Saved.")
        }
      }
    )
  }

  // A valid password must have at least one number, one or more characters,
  // at at least one punctuation character. Length is asserted elsewhere.
  private val ValidPassword = """^(?=.*\d)(?=.*[a-zA-Z])(?=.*[\-!@#$%^&*.,_:;])""".r

  private def validPassword(password: String) = {
    ValidPassword.findFirstIn(password).map {s =>
      (password.length >= MinPasswordLength) &&
      (password.length <= MaxPasswordLength)
    }.getOrElse(false)
  }

  private def uniqueUser(username: String) = {
    User.findByName(username) match {
      case Left(error) => true
      case Right(user) => false
    }
  }
}
