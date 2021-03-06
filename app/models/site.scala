package models

import play.api.libs.Crypto
import play.api.Logger
import play.api.libs.json._

case class Site(name:     String,
                username: Option[String],
                email:    Option[String],
                password: Option[String],
                url:      Option[String],
                notes:    Option[String],
                id:       Option[Long] = None) {

  // Useful when creating.
  def this(name:     String,
           username: Option[String] = None,
           email:    Option[String] = None,
           password: Option[String] = None,
           url:      Option[String] = None) = {
    this(name, username, email, password, url, None)
  }

  def this(name: String) = this(name, None, None, None, None, None)

  // Get the user associated with this site.

  // Convert this site object to JSON (a JsValue, not a string). To
  // convert this result to a JSON string, use this code:
  //
  //     import play.api.libs.json.Json
  //     Json.stringify(site.toJson)
  def toJson = Json.toJson(
    Map(
      "name"     -> Json.toJson(name),
      "username" -> Json.toJson(username.getOrElse("")),
      "email"    -> Json.toJson(email.getOrElse("")),
      "password" -> Json.toJson(password.getOrElse("")),
      "notes"    -> Json.toJson(notes.getOrElse("")),
      "url"      -> Json.toJson(url.getOrElse("")),
      "id"       -> Json.toJson(id.getOrElse(-1.toLong))
    )
  )
}

object Site {
  import anorm._
  import play.api.Play.current
  import ModelUtil._

  val Dummy = new Site("dummy")

  def findByID(id: Long, user: User): Either[String, Site] = {
    val sql = SQL("""|SELECT * FROM sites
                     |WHERE (id = {id}) AND (user_id = {uid})""".stripMargin).
              on("id"  -> id,
                 "uid" -> user.id.get)

    executeQuery(sql) { results =>
      decodeResults(results) match {
        case Nil         => Left("Not found")
        case item :: Nil => Right(item)
        case _           => Left("(BUG) Multiple sites with ID " + id)
      }
    }
  }

  def count(user: User): Either[String, Long] = {
    val sql = SQL("SELECT COUNT(id) AS count FROM sites WHERE user_id = {id}").
              on("id" -> user.id)

    executeQuery(sql) { results =>
      Right(results.map { row => row[Long]("count") }.toList.head)
    }
  }

  def allForUser(user: User): Either[String, Seq[Site]] = {
    val sql = SQL("SELECT * FROM sites WHERE user_id = {id} ORDER BY name").
              on("id" -> user.id)

    executeQuery(sql) { results => Right(decodeResults(results)) }
  }

  def all: Either[String, Seq[Site]] = {
    executeQuery(SQL("SELECT * FROM sites ORDER BY name")) { results =>
      Right(decodeResults(results))
    }
  }

  def create(site: Site, forUser: User): Either[String, Site] = {
    withDBConnection { implicit connection =>
      val sql = SQL(
        """|INSERT INTO sites(name, username, email, password, notes, user_id, url)
           |VALUES({name}, {username}, {email}, {pw}, {notes}, {uid}, {url})""".
           stripMargin).
        on("name"     -> site.name,
           "username" -> site.username,
           "email"    -> site.email,
           "pw"       -> site.password,
           "notes"    -> site.notes,
           "url"      -> site.url,
           "uid"      -> forUser.id.get)

      // Note that executeInsert() will return the primary key.
      // See http://stackoverflow.com/questions/9859700/
      sql.executeInsert() match {
        // Yes, I know I should map() here. This is more readable.
        case Some(id) => Right(site.copy(id = Some(id)))
        case None     => Left("No ID returned from database INSERT")
      }
    }
  }

  def update(site: Site): Either[String, Boolean] = {
    // flatMap() unpacks an Option, but expects one back. Thus, it's a
    // simple way to conditionally convert a Some("") into a None.
    val sql = SQL(
      """|UPDATE sites SET name = {name}, username = {username},
         |email = {email}, password = {password}, url = {url}, notes = {notes}
         |WHERE id = {id}""".stripMargin).
      on("name"     -> site.name,
         "username" -> site.username,
         "email"    -> site.email,
         "password" -> site.password,
         "url"      -> site.email,
         "notes"    -> site.notes,
         "id"       -> site.id.get
      )

    withDBConnection { implicit connection =>
      sql.executeUpdate()
      Right(true)
    }
  }

  def delete(id: Long): Either[String, Boolean] = {
    withDBConnection { implicit connection =>
      SQL("DELETE FROM sites WHERE id = {id}").on("id" -> id).executeUpdate()
      Right(true)
    }
  }

  def search(q: String, user: User): Either[String, Seq[Site]] = {
    val searchTerm = "%" + q.toLowerCase + "%"
    withDBConnection { implicit connection =>
      val sql = SQL(
        """|SELECT * FROM sites
           |WHERE user_id = {uid} AND (LOWER(name) LIKE {term})""".stripMargin).
        on("uid"  -> user.id.get,
           "term" -> searchTerm)

      executeQuery(sql) { results => Right(decodeResults(results)) }
    }
  }

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  private def decodeResults(results: Stream[Row]): Seq[Site] =
    results.map {decodeSite _}.toList

  private def decodeSite(row: Row): Site = {
    Site(row[String]("name"),
         row[Option[String]]("username"),
         row[Option[String]]("email"),
         row[Option[String]]("password"),
         row[Option[String]]("url"),
         row[Option[String]]("notes"),
         Some(row[Long]("id")))
  }
}