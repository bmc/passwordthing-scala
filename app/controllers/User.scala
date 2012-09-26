package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.json.Json

import models.User._
import models._

import controllers.util._

// Not the real user. Only used internally.

object UserController extends Controller with Secured with ControllerUtil {

  // ----------------------------------------------------------------------
  // Constants
  // ----------------------------------------------------------------------

  private val MinUsernameLength = 3
  private val MinPasswordLength = 8
  private val MaxPasswordLength = 32
  // A valid password must have at least one number, one or more characters,
  // at at least one punctuation character. Length is asserted elsewhere.
  private val ValidPassword = """^(?=.*\d)(?=.*[a-zA-Z])(?=.*[\-!@#$%^&*.,_:;])""".r
  private val PasswordError = 
    """|Passwords must be between %d and %d characters and must have at
       |least one letter, one number, and one punctuation character.""".
    stripMargin.format(MinPasswordLength, MaxPasswordLength)

  // ----------------------------------------------------------------------
  // Actions
  // ----------------------------------------------------------------------

  def index = ActionWithAdminUser { currentUser => implicit request =>
    Ok(views.html.users.index(currentUser))
  }

  def list = ActionWithAdminUser { currentUser => implicit request =>
    User.all.fold(
      { error => Ok(userJson(Nil, Some(error))) },
      { users => Ok(userJson(users)) }
    )
  }

  private val editUserForm = Form(
    mapping(
      "username"              -> nonEmptyText(minLength=MinUsernameLength),
      "password"              -> optional(text.verifying(PasswordError,
                                                         validPassword _)),
      "password_confirmation" -> optional(text.verifying(PasswordError,
                                                         validPassword _)),
      "first_name"            -> optional(text),
      "last_name"             -> optional(text),
      "email"                 -> optional(text),
      "isAdmin"               -> boolean
    )
    (applyForEdit)
    (unapplyForEdit)
    verifying("Passwords don't match.", passwordsMatch _)
  )

  /** Display the form to edit an existing user.
    */
  def edit(id: Long) = ActionWithAdminUser { currentUser => implicit request =>
    User.findByID(id).fold(
      { error =>

        Redirect(routes.UserController.index()).flashing("error" -> error) 
      },

      { user =>

        Ok(views.html.users.edit(user, currentUser, editUserForm.fill(user)))
      }
    )
  }

  /** Update a user in response to an edit operation.
    */
  def update(id: Long) = {
    ActionWithAdminUser(parse.urlFormEncoded) {
      currentUser => implicit request =>

      editUserForm.bindFromRequest.fold (

        // Failure. Re-post.
        { form =>

          User.findByID(id).fold(
            { error =>

              Redirect(routes.UserController.index()).
                flashing("error" -> ("Can't to find user with ID " + id))
            },
  
            { user =>

              BadRequest(views.html.users.edit(user, currentUser, form))
            }
          )
        },

        { user =>

          val userAndID = user.copy(id = Some(id))
println("*** userAndID=" + userAndID)
          // The ID isn't part of the form-built user. Use the case-class copy()
          // functionality to copy one into place.
          User.update(userAndID).fold(
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
              val filledForm = editUserForm.fill(userAndID)
              Ok(views.html.users.edit(userAndID, currentUser, filledForm)(flash))
            },

            { _ =>

              Redirect(routes.UserController.edit(id)).flashing("info" -> "Saved.")
            }
          )
        }
      )
    }
  }

  val newUserForm = Form(
    mapping(
      "username"              -> nonEmptyText(minLength=MinUsernameLength).
                                 verifying("User already exists", uniqueUser _),
      "password"              -> text.verifying(PasswordError, validPassword _),
      "password_confirmation" -> text.verifying(PasswordError, validPassword _),
      "first_name"            -> optional(text),
      "last_name"             -> optional(text),
      "email"                 -> optional(text),
      "isAdmin"               -> boolean
    )
    (applyForCreate)
    (unapplyForCreate)
    verifying("Passwords don't match.", passwordsMatch _)
  )

  /** Display the form to edit an existing user.
    */
  def makeNew = ActionWithAdminUser { currentUser => implicit request =>
    Ok(views.html.users.makeNew(currentUser, newUserForm))
  }

  /** Update a user in response to an edit operation.
    */
  def create = {
    ActionWithAdminUser(parse.urlFormEncoded) { currentUser => implicit request =>
      newUserForm.bindFromRequest.fold (

        // Failure. Re-post.
        { form =>
  
          BadRequest(views.html.users.makeNew(currentUser, form))
        },

        { user =>

          User.create(user).fold(
            { error =>

              val filledForm = newUserForm.fill(user)
              val flash = Flash(Map("error" -> error))
              Ok(views.html.users.makeNew(currentUser, filledForm)(flash))
            },

            { dbUser =>

              Redirect(routes.UserController.edit(dbUser.id.get)).
                flashing("info" -> "Saved.")
            }
          )
        }
      )
    }
  }

  /**
    * Delete a user by ID.
    */
  def delete(id: Long) = ActionWithAdminUser { currentUser => implicit request =>
    val error = User.delete(id).fold(
      { error => Some(error) },
      { bool  => None }
    )

    User.all.fold(
      { error2 =>

        // Combine the errors, assuming there's a first one.
        val e = error.map(_ + ", " + error2).getOrElse(error2)
        Ok(userJson(Nil, Some(e)))
      },

      { users => Ok(userJson(users, error)) }
    )
  }

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  private def applyForCreate(name:                 String,
                             password:             String,
                             passwordConfirmation: String,
                             firstName:            Option[String],
                             lastName:             Option[String],
                             email:                Option[String],
                             isAdmin:              Boolean) =
    User(name,
         User.encrypt(password),
         Some(password),
         Some(passwordConfirmation),
         firstName,
         lastName,
         email,
         isAdmin)

  private def unapplyForCreate(user: User) =
    Some((user.username, 
          user.password.getOrElse(""),
          user.passwordConfirmation.getOrElse(""),
          user.firstName,
          user.lastName,
          user.email,
          user.isAdmin))

  private def applyForEdit(name:                 String,
                           password:             Option[String],
                           passwordConfirmation: Option[String],
                           firstName:            Option[String],
                           lastName:             Option[String],
                           email:                Option[String],
                           isAdmin:              Boolean) = {
    User(name,
         password.map {pw => User.encrypt(pw)}.getOrElse(""),
         password,
         passwordConfirmation,
         firstName,
         lastName,
         email,
         isAdmin)
  }

  private def unapplyForEdit(user: User) =
    Some((user.username, 
          user.password,
          user.passwordConfirmation,
          user.firstName,
          user.lastName,
          user.email,
          user.isAdmin))

  private def userJson(users: Seq[User], errorMessage: Option[String] = None) = {
    val usersMap = Json.toJson(users.map {_.toJson})
    Json.stringify(
      Json.toJson(
        errorMessage.map(e =>
          Map("error" -> Json.toJson(e), "users" -> usersMap)
        ).getOrElse(
          Map("users" -> usersMap)
        )
      )
    )
  }

  private def validPassword(password: String) = {
    ValidPassword.findFirstIn(password).map {s =>
      (password.length >= MinPasswordLength) &&
      (password.length <= MaxPasswordLength)
    }.getOrElse(false)
  }

  private def passwordsMatch(user: User) = {
    user.password.getOrElse("") == user.passwordConfirmation.getOrElse("")
  }

  private def uniqueUser(username: String) = {
    User.findByName(username).fold(
      { error => true },
      { user  => false }
    )
  }
}
