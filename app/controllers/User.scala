package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.{Form, WrappedMapping}
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
    val count = User.count.fold(
      { error =>

        Logger.error("Can't get count of total number of users: " + error)
        None
      },

      { count => Some(count) }
    )

    Ok(views.html.users.index(currentUser, count))
  }

  def listJSON = ActionWithAdminUser { currentUser => implicit request =>
    User.all.fold(
      error => Ok(userJson(Nil, Some(error))),
      users => Ok(userJson(users))
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
    (UIUser.apply)
    (UIUser.unapply)
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

        val uiUser = new UIUser(user)
        val filledForm = editUserForm.fill(uiUser)
        Ok(views.html.users.edit(uiUser, id, currentUser, filledForm))
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

              val uiUser = new UIUser(user)
              BadRequest(views.html.users.edit(uiUser, id, currentUser, form))
            }
          )
        },

        { uiUser =>

          User.update(uiUser.toUser.copy(id = Some(id))).fold(
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
              val filledForm = editUserForm.fill(uiUser)
              Ok(views.html.users.edit(uiUser, id, currentUser, filledForm)(flash))
            },

            { _ =>

              Redirect(routes.UserController.edit(id)).
                flashing("info" -> "Saved.")
            }
          )
        }
      )
    }
  }

  // The new user form is similar, but not quite identical, to the
  // editUserForm. The password is required. Rather than write separate
  // apply/unapply methods, we'll just tack a new constraint on the end.

  private val newUserForm = Form(
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
    (UIUser.apply)
    (UIUser.unapply)
    verifying("Passwords don't match.", passwordsMatch _)
    verifying("Password is required.", _.password.getOrElse("").length > 0)
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

        { uiUser =>

          User.create(uiUser.toUser).fold(
            { error =>

              val filledForm = newUserForm.fill(uiUser)
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
      error => Some(error),
      bool  => None
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

  private def passwordsMatch(user: UIUser) = {
    user.password.getOrElse("") == user.passwordConfirmation.getOrElse("")
  }

  private def uniqueUser(username: String) = {
    User.findByUsername(username).fold(
      error => true,
      user  => false
    )
  }
}

/** This version of the User class exists solely to communicate with the
  * UI. It contains unencrypted passwords (which should not be persisted)
  * but it lacks an encrypted password and an ID. It's mapped to and from
  * the User model, when interacting with the database. But it's here,
  * rather than in models, because it's not a model; it's simply a UI 
  * artifact.
  */
sealed case class UIUser(username:             String,
                         password:             Option[String],
                         passwordConfirmation: Option[String],
                         firstName:            Option[String],
                         lastName:             Option[String],
                         email:                Option[String],
                         isAdmin:              Boolean) {
  def this(user: User) = {
    this(user.username, None, None, user.firstName, user.lastName,
         user.email, user.isAdmin)
  }

  def toUser = {
    User(username, User.encrypt(password.getOrElse("")), firstName, lastName,
         email, isAdmin)
  }
}