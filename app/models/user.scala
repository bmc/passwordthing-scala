package models

import play.api.libs.Crypto
import play.api.Logger
import play.api.libs.json._

case class User(username:             String,
                encryptedPassword:    String,
                password:             Option[String], // Not persisted
                passwordConfirmation: Option[String], // Not persisted
                firstName:            Option[String],
                lastName:             Option[String],
                email:                Option[String],
                isAdmin:              Boolean,
                id:                   Option[Long] = None) {

  // Helpful for creation
  def this(username: String, password: String, isAdmin: Boolean) =
    this(username, "", Some(password), Some(password), None, None, None, isAdmin)

  /** Get the friendliest possible name for this user, for display purposes.
    */
  lazy val displayName = {
    val fullName = List(firstName, lastName).flatten.mkString(" ").trim
    if (fullName.length > 0) fullName else username
  }

  /** Convert this user object to JSON (a JsValue, not a string). To
    * convert this result to a JSON string, use this code:
    *
    * import play.api.libs.json.Json
    * Json.stringify(user.toJson)
    */
  lazy val toJson = Json.toJson(
    Map(
      "username"   -> Json.toJson(username),
      "email"      -> Json.toJson(email),
      "first_name" -> Json.toJson(firstName),
      "last_name"  -> Json.toJson(lastName),
      "is_admin"   -> Json.toJson(isAdmin),
      "id"         -> Json.toJson(id.getOrElse(-1.toLong))
    )
  )
}

object User {
  import anorm._
  import play.api.Play.current
  import ModelUtil._

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

  def count: Either[String, Long] = {
    executeQuery(SQL("SELECT COUNT(id) AS count FROM appusers")) { results =>
      Right(results.map { row => row[Long]("count") }.toList.head)
    }
  }

  def create(user: User): Either[String, User] = {
    withDBConnection { implicit connection =>
      val sql = SQL(
        """|INSERT INTO appusers(username, encrypted_password, first_name,
           |last_name, email, is_admin)
           |VALUES({name}, {pw}, {first_name}, {last_name}, {email},
           |{admin})""".stripMargin).
        on("name"       -> user.username,
           "pw"         -> encrypt(user.password.getOrElse("")),
           "first_name" -> user.firstName,
           "last_name"  -> user.lastName,
           "email"      -> user.email,
           "admin"      -> encodeBoolean(user.isAdmin))

      // Note that executeInsert() will return the primary key.
      // See http://stackoverflow.com/questions/9859700/
      sql.executeInsert() match {
        // Yes, I know I should map() here. This is more readable.
        case Some(id) => Right(user.copy(id = Some(id)))
        case None     => Left("No ID returned from database INSERT")
      }
    } 
  }

  def update(user: User): Either[String, Boolean] = {
    // flatMap() unpacks an Option, but expects one back. Thus, it's a
    // simple way to conditionally convert a Some("") into a None.
    val sql = user.password.flatMap { pw =>
      if (pw == "") None else Some(pw)
    }.map { pw =>
      // Within this block, we know we actually have a password.
      SQL("""|UPDATE appusers
             |SET username = {name}, encrypted_password = {pw},
             |first_name = {first_name}, last_name = {last_name},
             |email = {email}, |is_admin = {admin}
             |WHERE id = {id}""".stripMargin).
      on("name"       -> user.username,
         "pw"         -> encrypt(pw),
         "first_name" -> user.firstName,
         "last_name"  -> user.lastName,
         "email"      -> user.email,
         "admin"      -> encodeBoolean(user.isAdmin),
         "id"         -> user.id.get
      )
    }.getOrElse(
      // Within this block, we know we don't.
      SQL("""|UPDATE appusers
             |SET username = {name}, first_name = {fname}, last_name = {lname},
             |email = {email}, is_admin = {admin}
             |WHERE id = {id}""".stripMargin).
      on("name"   -> user.username,
         "admin"  -> encodeBoolean(user.isAdmin),
         "fname"  -> user.firstName,
         "lname"  -> user.lastName,
         "email"  -> user.email,
         "id"     -> user.id.get
      )
    )

    withDBConnection { implicit connection =>
      sql.executeUpdate()
      Right(true)
    }
  }

  def delete(id: Long): Either[String, Boolean] = {
    withTransaction { implicit connection =>
      SQL("DELETE FROM sites WHERE user_id = {id}").on("id" -> id).executeUpdate()
      SQL("DELETE FROM appusers WHERE id = {id}").on("id" -> id).executeUpdate()
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
         row[Option[String]]("first_name"),
         row[Option[String]]("last_name"),
         row[Option[String]]("email"),
         decodeBoolean(row[Int]("is_admin")),
         Some(row[Long]("id")))
  }
}
