package models

import play.api.libs.Crypto
import play.api.Logger
import play.api.libs.json._

case class User(username:             String,
                encryptedPassword:    String,
                password:             Option[String], // Not persisted
                passwordConfirmation: Option[String], // Not persisted
                isAdmin:              Boolean,
                id:                   Option[Long] = None) {

  // Convert this user object to JSON (a JsValue, not a string). To
  // convert this result to a JSON string, use this code:
  //
  //     import play.api.libs.json.Json
  //     Json.stringify(user.toJson)
  def toJson = Json.toJson(
    Map(
      "username" -> Json.toJson(username),
      "isAdmin"  -> Json.toJson(isAdmin),
      "id"       -> Json.toJson(id.getOrElse(-1.toLong))
    )
  )
}

object User {
  import anorm._
  import play.api.db.DB
  import play.api.Play.current

  /** Find a user by name. Returns the user object (Right(User)) or a log
    * message (Left(String)). The log message is not intended to be shown to
    * the user.
    */
  def findByName(name: String): Either[String, User] = {
    DB.withConnection { implicit connection =>
      val query = SQL("SELECT * FROM user WHERE username = {name}").
                  on("name" -> name)
      query.apply().map {decodeUser _}.toList match {
        case user :: users :: Nil => Left("(BUG) Multiple users match username!")
        case user :: Nil          => Right(user)
        case Nil                  => Left("Unknown user: \"" + name + "\"")
      }
    }
  }

  def findByID(id: Long): Either[String, User] = {
    DB.withConnection { implicit connection =>
      val query = SQL("SELECT * FROM user WHERE id = {id}").on("id" -> id)

      query.apply().map {decodeUser _}.toList match {
        case user :: users :: Nil => Left("(BUG) Multiple users match that ID!")
        case user :: Nil          => Right(user)
        case Nil                  => Left("Unknown user ID: \"" + id + "\"")
      }
    }
  }

  // Retrieve all users in the database, ordered by name.
  def all: Seq[User] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM user ORDER BY username").apply().toList
    }.map {decodeUser _}
  }

  def create(name: String, password: String, isAdmin: Boolean): Option[User] = {
    val user = User(name, encrypt(password), None, None, isAdmin)
    try {
      DB.withConnection { implicit connection =>
        SQL(
          """
          INSERT INTO user(username, encrypted_password, is_admin)
          VALUES({name}, {pw}, {admin})
          """
        ).on(
          "name"  -> user.username,
          "pw"    -> user.encryptedPassword,
          "admin" -> (if (user.isAdmin) 1 else 0)
        ).executeInsert()
      }

      Some(user)
    }

    catch {
      case e: java.sql.SQLException =>
        Logger.error("Failed to create user: " + e.getMessage)
        None
    }
  }

  def update(user: User): Either[String, Boolean] = {
    val userToSave = user.password.map { pw =>
      User(user.username,
           encrypt(pw),
           Some(pw),
           Some(pw),
           user.isAdmin,
           user.id)
    }.getOrElse(user)

    id = userToSave.id.get // Must be there.
    try {
      DB.withConnection { implicit connection =>
        val sql = user.password match {
          case None =>
            SQL("UPDATE user SET name = {name}, is_admin ")
        }
      }
    }

    catch {
      case e: java.sql.Exception =>
        msg = "Failed to update user with ID %s: %d".format(
          user.id.get, e.getMessage
        )
        Logger.error(msg)
        Left(msg)
    }
  }

  def applyForEdit(name:                 String,
                   password:             String,
                   passwordConfirmation: String,
                   isAdmin:              Boolean,
                   id:                   Long) = {
    User(name,
         encrypt(password),
         Some(password),
         Some(passwordConfirmation),
         isAdmin)
  }

  def unapplyForEdit(user: User) =
    Some((user.username, 
          user.password.getOrElse(""),
          user.passwordConfirmation.getOrElse(""),
          user.isAdmin,
          user.id.getOrElse(0.toLong)))

  def applyForLogin(name: String, password: String): User =
    User(name, encrypt(password), None, None, false)

  def unapplyForLogin(user: User) =
    Some((user.username, user.encryptedPassword))

  private def decodeUser(row: SqlRow): User = {
    User(row[String]("username"),
         row[String]("encrypted_password"),
         None,
         None,
         decodeBoolean(row[Int]("is_admin")),
         Some(row[Int]("id")))
  }

  private def decodeBoolean(value: Int) = if (value == 0) false else true

  private def encrypt(password: String) = Crypto.sign(password)
}
