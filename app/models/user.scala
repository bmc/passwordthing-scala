package models

import play.api.libs.Crypto
import play.api.Logger

case class User(
  username: String,
  encryptedPassword: String,
  isAdmin: Boolean = false
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
      query.apply().map {row =>
        User(
          row[String]("username"),
          row[String]("encrypted_password"),
          if (row[Int]("is_admin") == 0) false else true
        )
      }.toList match {
        case user :: users :: Nil => Left("Multiple users match that username!")
        case user :: Nil          => Right(user)
        case Nil                  => Left("Unknown user: \"" + name + "\"")
      }
    }
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
    User(name, Crypto.sign(password))

  def unapplyNamePassword(user: User) =
    Some((user.username, user.encryptedPassword))
}
