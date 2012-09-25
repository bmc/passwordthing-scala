package models

import anorm._
import play.api.Logger
import play.api.db.DB
import play.api.Play.current

trait ModelUtil {

  /** Execute a SQL query, logging it at DEBUG level. Any SQL exceptions
    * are appropriate mapped and returned as the left-hand value.
    * The result of the call to the apply() method is the right-hand side.
    */
  def executeQuery[T](sql: SimpleSql[Row])
                     (code: Stream[Row] => Either[String,T]): Either[String,T] = {
    try {
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

  /** Run a block of code within a database connection.
    */
  def withDBConnection[T](code: java.sql.Connection => Either[String,T]):
  Either[String, T] = {
    try {
      DB.withConnection { implicit connection =>
        code(connection)
      }
    }

    catch {
      case e: java.sql.SQLException =>
        Logger.error(e.getMessage)
        Left(e.getMessage)
    }
  }

}
