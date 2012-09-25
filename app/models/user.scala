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

object User extends ModelUtil {
  import anorm._
  import play.api.Play.current

  /** Find a user by name. Returns the user object (Right(User)) or a log
    * message (Left(String)). The log message is not intended to be shown to
    * the user.
    */
  def findByName(name: String): Either[String, User] = {
    val query = SQL(
       "SELECT * FROM appusers WHERE username = {name}"
    ).on("name" -> name)

    executeQuery(query) { results =>
      results.map {decodeUser _}.toList match {
        case Nil =>
          Left("There is no user with name \"" + name + "\"")
        case user :: Nil =>
          Right(user)
        case user :: more :: Nil =>
          val msg = "(BUG) More than one user with name \"" + name + "\""
          Logger.debug(msg)
          Left(msg)
      }
    }
  }

  def findByID(id: Long): Either[String, User] = {
    val query = SQL("SELECT * FROM appusers WHERE id = {id}").on("id" -> id)

    executeQuery(query) { results =>
      results.map {decodeUser _}.toList match {
        case Nil =>
          Left("There is no user with ID \"" + id + "\"")
        case user :: Nil =>
          Right(user)
        case user :: more :: Nil =>
          val msg = "(BUG) More than one user with ID \"" + id + "\""
          Logger.debug(msg)
          Left(msg)
      }
    }
  }

  // Retrieve all users in the database, ordered by name.
  def all: Either[String, Seq[User]] = {
    executeQuery(SQL("SELECT * FROM appusers ORDER BY username")) { results =>
      Right(results.toList.map {decodeUser _})
    }
  }

  def create(name: String,
             password: String,
             isAdmin: Boolean): Either[String, User] = {

    val user = User(name, encrypt(password), None, None, isAdmin)
    withDBConnection { implicit connection =>
      val sql = SQL(
        """
        INSERT INTO appusers(username, encrypted_password, is_admin)
        VALUES({name}, {pw}, {admin})
        """
      ).on(
        "name"  -> user.username,
        "pw"    -> user.encryptedPassword,
        "admin" -> encodeBoolean(user.isAdmin)
      )

      sql.executeInsert()

      // Reload, to get the ID.
      findByName(name) match {
        case Left(error) =>
          val msg = "Couldn't reload user after save: %s".format(error)
          Logger.error(msg)
          Left(msg)

        case Right(user) =>
          Right(user)
      }
    }
  }

  def update(user: User): Either[String, Boolean] = {
    // flatMap() unpacks an Option, but expects one back. Thus, it's a
    // simply way to conditionally convert a Some("") into a None.
    val sql = user.password.flatMap { pw =>
      if (pw == "") None else Some(pw)
    }.map { pw =>
      // Within this block, we know we actually have a password.
      SQL("""|UPDATE appusers
             |SET username = {name}, encrypted_password = {pw},
             |is_admin = {admin}
             |WHERE id = {id}""".stripMargin).
      on("name"  -> user.username,
         "pw"    -> encrypt(pw),
         "admin" -> encodeBoolean(user.isAdmin),
         "id"    -> user.id.get
      )
    }.getOrElse(
      // Within this block, we know we don't.
      SQL("""|UPDATE appusers SET username = {name}, is_admin = {admin}
             |WHERE id = {id}""".stripMargin).
      on("name"  -> user.username,
         "admin" -> encodeBoolean(user.isAdmin),
         "id"    -> user.id.get
      )
    )

    withDBConnection { implicit connection =>
      sql.executeUpdate()
      Right(true)
    }
  }

  def delete(id: Long): Either[String, Boolean] = {
    withDBConnection { implicit connection =>
      val sql = SQL("DELETE FROM appusers WHERE id = {id}").on("id" -> id)
      sql.executeUpdate()
      Right(true)
    }
  }

  // Encrypt a password.
  def encrypt(password: String) = Crypto.sign(password)

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  private def decodeUser(row: Row): User = {
    User(row[String]("username"),
         row[String]("encrypted_password"),
         None,
         None,
         decodeBoolean(row[Int]("is_admin")),
         Some(row[Long]("id")))
  }
}
