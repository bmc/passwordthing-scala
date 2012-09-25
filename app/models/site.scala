package models

import play.api.libs.Crypto
import play.api.Logger
import play.api.libs.json._

case class Site(name:                 String,
                username:             Option[String],
                email:                Option[String],
                password:             Option[String],
                notes:                Option[String],
                userID:               Long,
                id:                   Option[Long] = None)

