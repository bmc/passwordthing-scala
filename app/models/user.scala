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
          "admin" -> encodeBoolean(user.isAdmin)
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
    // flatMap() unpacks an Option, but expects one back. Thus, it's a
    // simply way to conditionally convert a Some("") into a None.
    val sql = user.password.flatMap { pw =>
      if (pw == "") None else Some(pw)
    }.map { pw =>
      // Within this block, we know we actually have a password.
      SQL("""|UPDATE user
             |SET name = {name}, encrypted_password = {pw}, is_admin = {admin}
             |WHERE id = {id}""".stripMargin).
      on("name"  -> user.username,
         "pw"    -> encrypt(pw),
         "admin" -> encodeBoolean(user.isAdmin),
         "id"    -> user.id.get
      )
    }.getOrElse(
      // Within this block, we know we don't.
      SQL("""|UPDATE user SET username = {name}, is_admin = {admin}
             |WHERE id = {id}""".stripMargin).
      on("name"  -> user.username,
         "admin" -> encodeBoolean(user.isAdmin),
         "id"    -> user.id.get
      )
    )

    try {
      DB.withConnection { implicit connection =>
        sql.executeUpdate()
        Right(true)
      }
    }

    catch {
      case e: java.sql.SQLException =>
        val msg = "Failed to update user with ID %d: %s".format(
          user.id.get, e.getMessage
        )
        Logger.error(msg)
        Left(msg)
    }
  }

  def applyForCreate(name:                 String,
                     password:             String,
                     passwordConfirmation: String,
                     isAdmin:              Boolean) =
    User(name,
         encrypt(password),
         Some(password),
         Some(passwordConfirmation),
         isAdmin)

  def unapplyForCreate(user: User) =
    Some((user.username, 
          user.password.getOrElse(""),
          user.passwordConfirmation.getOrElse(""),
          user.isAdmin))

  def applyForEdit(name:                 String,
                   password:             Option[String],
                   passwordConfirmation: Option[String],
                   isAdmin:              Boolean,
                   id:                   Long) = {
    User(name,
         password.map {pw => encrypt(pw)}.getOrElse(""),
         password,
         passwordConfirmation,
         isAdmin,
         Some(id))
  }

  def unapplyForEdit(user: User) =
    Some((user.username, 
          user.password,
          user.passwordConfirmation,
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

  private def encodeBoolean(value: Boolean) = if (value) 1 else 0

  private def encrypt(password: String) = Crypto.sign(password)
}
