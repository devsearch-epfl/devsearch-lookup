package devsearch.lookup

import reactivemongo.api.{MongoConnection, MongoDriver}

import scala.concurrent.ExecutionContext.Implicits.global

object RawDB {
  val DB_SERVER = "localhost"
  val DB_NAME = "devsearch"

  val driver = new MongoDriver
  val connection : MongoConnection = driver.connection(List(DB_SERVER))
  val db = connection(DB_NAME)
}
