package devsearch.lookup

import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.RawCommand

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RawDB {
  val DB_SERVER = "localhost"
  val DB_NAME = "devsearch"

  val driver = new MongoDriver
  val connection = driver.connection(List(DB_SERVER))
  val db = connection(DB_NAME)

  def getCollection(collectionName: String):BSONCollection = db(collectionName)

  def run(command: BSONDocument): Future[BSONDocument] = db.command(RawCommand(command))
}
