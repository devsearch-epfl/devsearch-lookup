package devsearch

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.ActorLogging
import akka.actor.Actor
import akka.pattern.pipe
import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.bson._
import scala.concurrent.Future

class DummySearchActor extends Actor with ActorLogging {

  val driver = new MongoDriver
  val connection = driver.connection(List("localhost"))
  val db = connection("devsearch")
  val collection = db("features")

  // returns future to tuple (repo, file path, line)
  def getMatchesFromDb(features: Seq[String]): Future[Seq[(String, String, Long)]] = {
    val query = BSONDocument("$or" -> BSONArray(
      features.map(f => BSONDocument("feature" -> f))
    ))
    val filter = BSONDocument("_id" -> 0, "file" -> 1, "line" -> 1)
    val matchesFuture: Future[List[BSONDocument]] =
      collection.find(query, filter).cursor[BSONDocument].collect[List](10)
    matchesFuture.map{ list =>
      list.map { doc =>
        val repoAndFile = doc.getAs[String]("file").get
        val firstSlash = repoAndFile.indexOf("/")
        val secondSlash = repoAndFile.indexOf("/", firstSlash + 1)
        val repo = repoAndFile.substring(0, secondSlash)
        val file = repoAndFile.substring(secondSlash + 1)
        (repo, file, doc.getAs[Long]("line").get)
      }.toSeq
    }
  }

  @override
  def receive = {
    case features: List[String @unchecked] =>
      val result = getMatchesFromDb(features).map(Right(_)).recover({
        case e => Left(e.getMessage)
      })
      result pipeTo sender
  }
}
