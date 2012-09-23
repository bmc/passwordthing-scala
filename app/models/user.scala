package models

import play.api.libs.Crypto
import play.api.Logger

case class User(
  username: String,
  encryptedPassword: String,
  isAdmin: Boolean
)

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
        case user :: users :: Nil => Left("Multiple users match that username!")
        case user :: Nil          => Right(user)
        case Nil                  => Left("Unknown user: \"" + name + "\"")
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
    val user = User(name, Crypto.sign(password), isAdmin)
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

  def applyNamePassword(name: String, password: String): User =
    User(name, Crypto.sign(password), false)

  def unapplyNamePassword(user: User) =
    Some((user.username, user.encryptedPassword))

  private def decodeUser(row: SqlRow): User = {
    User(row[String]("username"),
         row[String]("encrypted_password"),
         decodeBoolean(row[Int]("is_admin")))
  }

  private def decodeBoolean(value: Int) = if (value == 0) false else true
}
