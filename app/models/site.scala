package models

import play.api.libs.Crypto
import play.api.Logger
import play.api.libs.json._

case class Site(name:                 String,
                username:             Option[String],
                email:                Option[String],
                password:             Option[String],
                url:                  Option[String],
                notes:                Option[String],
                userID:               Long,
                id:                   Option[Long] = None) {

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
      "userID"   -> Json.toJson(userID),
      "id"       -> Json.toJson(id.getOrElse(-1.toLong))
    )
  )
}


object Site extends ModelUtil {
  import anorm._
  import play.api.Play.current

  def findByID(id: Long): Either[String, Site] = {
    val query = SQL("SELECT * FROM sites WHERE id = {id}").on("id" -> id)

    executeQuery(query) { results =>
      Right(results.map {decodeSite _}.toList.head)
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

    executeQuery(sql) { results =>
      Right(results.toList.map {decodeSite _})
    }
  }

  def all: Either[String, Seq[Site]] = {
    executeQuery(SQL("SELECT * FROM sites ORDER BY name")) { results =>
      Right(results.toList.map {decodeSite _})
    }
  }

  // ----------------------------------------------------------------------
  // Private methods
  // ----------------------------------------------------------------------

  private def decodeSite(row: Row): Site = {
    Site(row[String]("name"),
         row[Option[String]]("username"),
         row[Option[String]]("email"),
         row[Option[String]]("password"),
         row[Option[String]]("url"),
         row[Option[String]]("notes"),
         row[Long]("user_id"),
         Some(row[Long]("id")))
  }
}