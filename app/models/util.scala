package models

import anorm._
import play.api.Logger
import play.api.db.DB
import play.api.Play.current

trait ModelUtil {

  /** Encode a boolean value for storage in the database.
    */
  def decodeBoolean(value: Int) = if (value == 0) false else true

  /** Decode a boolean value read from the database.
    */
  def encodeBoolean(value: Boolean) = if (value) 1 else 0

  /** Convert a SQL object, as returned by Anorm, to something more readable
    * (and loggable) than the regular SQL.toString method.
    */
  def sqlToString(sql: SimpleSql[Row]) = {
    sql.sql.query + " -> [" + 
    sql.params.map {t => t._1 + "=" + t._2.aValue}.mkString(", ") +
    "]"
  }

  /** Execute a SQL query, logging it at DEBUG level. Any SQL exceptions
    * are appropriate mapped and returned as the left-hand value.
    * The result of the call to the apply() method is the right-hand side.
    */
  def executeQuery[T](sql: SimpleSql[Row])
                     (code: Stream[Row] => Either[String,T]): Either[String, T] = {
    try {
      Logger.debug(sqlToString(sql))
      DB.withConnection { implicit connection =>
        code(sql())
      }
    }

    catch {
      case e: java.sql.SQLException =>
        val msg = "SQL query failed: " + e.getMessage
        Logger.error(msg)
        Left(msg)
    }
  }
}
