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

  /** Execute a hunk of code within a transaction. This can be used in place
    * of withDBConnection.
    * 
    * The transaction is committed, unless an exception is thrown or the
    * code returns Left(error).
    */
  def withTransaction[T](code: java.sql.Connection => Either[String,T]):
  Either[String, T] = {
    val connection = play.db.DB.getConnection
    val autoCommit = connection.getAutoCommit

    try {
      connection.setAutoCommit(false)
      val result = code(connection)
      result match {
        case Left(error)  => throw new Exception(error)
        case Right(_)     => connection.commit()
      }

      result
    }

    catch {
      case e: Throwable =>
        connection.rollback()
        val msg = "Error, rolling back transaction: " + e.getMessage
        Logger.error(msg)
        Left(msg)
    }

    finally {
      connection.setAutoCommit(autoCommit)
    }
  }

  /** Convert a boolean for storage in the database. Assumes the lowest
    * common denominator type, namely, integer.
    */
  def encodeBoolean(value: Boolean): Int = if (value) 1 else 0


  /** Convert a boolean value in the database to a Scala boolean. Assumes the
    * lowest common denominator type, namely, integer.
    */
  def decodeBoolean(dbValue: Int): Boolean = if (dbValue == 0) false else true
}
