package models

import play.api.libs.Crypto
import play.api.Logger
import play.api.libs.json._

case class User(username:             String,
                encryptedPassword:    String,
                password:             Option[String], // Not persisted
                passwordConfirmation: Option[String], // Not persisted
                isAdmin:              Boolean,
                id:                   Option[Int] = None) {

  // Convert this user object to JSON (a JsValue, not a string). To
  // convert this result to a JSON string, use this code:
  //
  //     import play.api.libs.json.Json
  //     Json.stringify(user.toJson)
  def toJson = Json.toJson(
    Map(
      "username" -> Json.toJson(username),
      "isAdmin"  -> Json.toJson(isAdmin),
      "id"       -> Json.toJson(id.getOrElse(-1))
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
    val user = User(name, Crypto.sign(password), None, None, isAdmin)
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
        Logger.debug("Failed to create user: " + e.getMessage)
        None
    }
  }

  def applyForEdit(name:                 String,
                   password:             String,
                   passwordConfirmation: String,
                   isAdmin:              Boolean) = {
    User(name,
         Crypto.sign(password),
         Some(password),
         Some(passwordConfirmation),
         isAdmin)
  }

  def unapplyForEdit(user: User) =
    Some((user.username, 
          user.password.getOrElse(""),
          user.passwordConfirmation.getOrElse(""),
          user.isAdmin))

  def applyForLogin(name: String, password: String): User =
    User(name, Crypto.sign(password), None, None, false)

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
}
