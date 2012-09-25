package controllers.admin

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.json.Json

import models.User._
import models._

import controllers._
import controllers.util._

// Not the real user. Only used internally.

object UserAdmin extends Controller with Secured with ControllerUtil {

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

  def listUsers = ActionWithAdminUser { user => implicit request =>
    User.all match {
      case Left(error)  => Ok(userJson(Nil, Some(error)))
      case Right(users) => Ok(userJson(users))
    }
  }

  private val editUserForm = Form(
    mapping(
      "username"              -> nonEmptyText(minLength=MinUsernameLength),
      "password"              -> optional(text.verifying(PasswordError,
                                                         validPassword _)),
      "password_confirmation" -> optional(text.verifying(PasswordError,
                                                         validPassword _)),
      "isAdmin"               -> boolean,
      "id"                    -> longNumber
    )
    (applyForEdit)
    (unapplyForEdit)
    verifying("Passwords don't match.", passwordsMatch _)
  )

  /** Display the form to edit an existing user.
    */
  def editUser(id: Long) = ActionWithAdminUser { currentUser => implicit request =>
    User.findByID(id) match {
      case Left(error) =>
        Redirect(admin.routes.Admin.index()).flashing("error" -> error)
      case Right(user) =>
        Ok(views.html.admin.edituser(user, currentUser, editUserForm.fill(user)))
    }
  }

  /** Update a user in response to an edit operation.
    */
  def updateUser = ActionWithAdminUser(parse.urlFormEncoded) {
    currentUser => implicit request =>

    editUserForm.bindFromRequest.fold (

      // Failure. Re-post.
      { form =>

        val id = form("id").value.get.toInt
        User.findByID(id) match {
          case Left(error) =>
            Redirect(admin.routes.Admin.index()).
              flashing("error" -> ("Unable to find user with ID " + id.toString))

          case Right(user) =>
            BadRequest(views.html.admin.edituser(user, currentUser, form))
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
            Redirect(admin.routes.UserAdmin.editUser(user.id.get)).
              flashing("info" -> "Saved.")
        }
      }
    )
  }

  val newUserForm = Form(
    mapping(
      "username"              -> nonEmptyText(minLength=MinUsernameLength).
                                 verifying("User already exists", uniqueUser _),
      "password"              -> text.verifying(PasswordError, validPassword _),
      "password_confirmation" -> text.verifying(PasswordError, validPassword _),
      "isAdmin"               -> boolean
    )
    (applyForCreate)
    (unapplyForCreate)
    verifying("Passwords don't match.", passwordsMatch _)
  )

  /** Display the form to edit an existing user.
    */
  def newUser = ActionWithAdminUser { currentUser => implicit request =>
    Ok(views.html.admin.newuser(currentUser, newUserForm))
  }

  /** Update a user in response to an edit operation.
    */
  def createUser = {
    ActionWithAdminUser(parse.urlFormEncoded) { currentUser => implicit request =>
      newUserForm.bindFromRequest.fold (

        // Failure. Re-post.
        { form =>
  
          BadRequest(views.html.admin.newuser(currentUser, form))
        },

        { user =>
          User.create(user.username, user.password.get, user.isAdmin) match {
            case Left(error) =>
            val filledForm = newUserForm.fill(user)
            val flash = Flash(Map("error" -> error))
            Ok(views.html.admin.newuser(currentUser, filledForm)(flash))

            case Right(dbUser) => {
              Redirect(admin.routes.UserAdmin.editUser(dbUser.id.get)).
              flashing("info" -> "Saved.")
            }
          }
        }
      )
    }
  }

  /**
    * Delete a user by ID.
    */
  def deleteUser(id: Long) = ActionWithAdminUser { currentUser => implicit request =>
    User.delete(id) match {
      case Left(error) => Ok(userJson(Nil, Some(error)))
      case Right(bool) =>
        User.all match {
          case Left(error)  => Ok(userJson(Nil, Some(error)))
          case Right(users) => Ok(userJson(users))
        }
    }
  }

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  private def applyForCreate(name:                 String,
                             password:             String,
                             passwordConfirmation: String,
                             isAdmin:              Boolean) =
    User(name,
         User.encrypt(password),
         Some(password),
         Some(passwordConfirmation),
         isAdmin)

  private def unapplyForCreate(user: User) =
    Some((user.username, 
          user.password.getOrElse(""),
          user.passwordConfirmation.getOrElse(""),
          user.isAdmin))

  private def applyForEdit(name:                 String,
                           password:             Option[String],
                           passwordConfirmation: Option[String],
                           isAdmin:              Boolean,
                           id:                   Long) = {
    User(name,
         password.map {pw => User.encrypt(pw)}.getOrElse(""),
         password,
         passwordConfirmation,
         isAdmin,
         Some(id))
  }

  private def unapplyForEdit(user: User) =
    Some((user.username, 
          user.password,
          user.passwordConfirmation,
          user.isAdmin,
          user.id.getOrElse(0.toLong)))

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
    User.findByName(username) match {
      case Left(error) => true
      case Right(user) => false
    }
  }
}
