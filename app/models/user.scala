package models

case class User(username: String, password: String)

object User {
  def findByName(name: String): Option[User] = {
    None
  }
}
