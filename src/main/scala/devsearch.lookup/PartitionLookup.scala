package devsearch.lookup

import akka.actor._
import akka.pattern.pipe
import reactivemongo.api._
import reactivemongo.bson._
import scala.concurrent.Future

/**
 * This actor is permanently responsible for one partition of the database. It
 * receives requests and returns the K best matches of the partition.
 */
class PartitionLookup extends Actor with ActorLogging {
  import context.dispatcher

  log.info("Starting PartitionLookup")

  val driver = new MongoDriver
  val connection = driver.connection(List("localhost"))
  val db = connection("devsearch")
  val collection = db("features")

  def getScore(repo: String, file: String, line: Int): Float = {
    line // TODO we need some scoring function here, for now just anything that has proper type
  }

  def getMatchesFromDb(features: Seq[String]): Future[SearchResult] = {
    if (features.isEmpty) Future.successful(SearchResultSuccess(Seq()))
    else {
      val query = BSONDocument("$or" -> BSONArray(
        features.map(f => BSONDocument("feature" -> f))
      ))
      val filter = BSONDocument("_id" -> 0, "file" -> 1, "line" -> 1)
      val matchesFuture: Future[List[BSONDocument]] =
        collection.find(query, filter).cursor[BSONDocument].collect[List](10)
      matchesFuture.map{ list =>
        SearchResultSuccess(FindNBest(list.map { doc =>
          val repoAndFile = doc.getAs[String]("file").get
          val firstSlash = repoAndFile.indexOf("/")
          val secondSlash = repoAndFile.indexOf("/", firstSlash + 1)
          val repo = repoAndFile.substring(0, secondSlash)
          val file = repoAndFile.substring(secondSlash + 1)
          val line = doc.getAs[Long]("line").get.toInt
          val score = getScore(repo, file, line)
          SearchResultEntry(repo, file, line, score)
        }, 10))
      }
    }
  }

  override def receive = {
    case SearchRequest(features) =>
      log.info("PartitionLookup: receive SearchRequest")

      val result: Future[SearchResult] = getMatchesFromDb(features)
        .recover({
          case e => SearchResultError(e.getMessage)
        })
      result pipeTo sender
    case x => log.error(s"Received unexpected message $x")
  }
}
