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

  def getMatchesFromDb(features: Seq[String]): Future[SearchResult] = {
    if (features.isEmpty)
      Future.successful(SearchResultSuccess(Seq()))
    else
    {
      val query = BSONDocument("$or" -> BSONArray(
        features.map(f => BSONDocument("feature" -> f))
      ))
      val filter = BSONDocument("_id" -> 0, "file" -> 1, "line" -> 1, "feature" -> 1)
      val matchesFuture: Future[List[BSONDocument]] =
        collection.find(query, filter).cursor[BSONDocument].collect[List](10)
      matchesFuture.map{ list =>
        SearchResultSuccess(FindNBest(list.map { doc =>
          val repoAndFile = doc.getAs[String]("file").get
          val firstSlash = repoAndFile.indexOf("/")
          val secondSlash = repoAndFile.indexOf("/", firstSlash + 1)
          val owner = repoAndFile.substring(0, firstSlash)
          val repo = repoAndFile.substring(firstSlash + 1, secondSlash)
          val file = repoAndFile.substring(secondSlash + 1)
          val line = doc.getAs[Long]("line").get.toInt
          //val score = getScore(repo, file, line)

          //get feature type and name
          val feature = doc.getAs[String]("feature").get


          new FeatureEntry(Location(owner, repo, file), feature, line)
        }.groupBy(_.loc).flatMap(getScores).toSeq, 10))
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


  /**
   * Generates superfeatures of each file and returns a searchResultEntry including score.
   * @param entry
   * @return
   */
  def getScore(entry: (Location, List[FeatureEntry])): SearchResultEntry = entry match{
    case (Location(owner, repo, file), list) => {

      val nbMatches = list.length

      val nbDifferentFeatures = list.groupBy(_.feature).toList.length


      //TODO: What kind of metric should we use here? we might need filesize for normalizing...
      //standard deviation is a bad choice!
      val lineAvg = list.map(_.line).sum/list.size
      val stdDevLines = Math.sqrt((list.map(e => Math.pow(e.line - lineAvg, 2)).sum)/(list.size - 1))


      //TODO: load repoRank into Mongo, query it or this file.
      val repoRank = 0

      //TODO: assign good weights!
      val score = 5 * nbMatches + 2 * nbDifferentFeatures - stdDevLines + repoRank



      print("\n\t"+file+" score: "+score+" ("+nbMatches+", "+nbDifferentFeatures+", "+10/stdDevLines+")\n\nS")



      SearchResultEntry(owner, repo, file, 0, score.toFloat)

    }
  }

  def clamp(x: Double, min: Double, max: Double): Double = if (x < min) min else if (x > max) max else x

  def getScores(entry: (Location, List[FeatureEntry])): Iterable[SearchResultEntry] = entry match {


    case (location, features) => {
      // TODO: Cluster epsilon should maybe depend on the language of the file?
      //       Typically scala features will be much closer to each other than in Java...
      val positions = features.map(_.line).toArray
      val clusters = DBSCAN(positions, 5.0, positions.length min 3)

      clusters.map { cluster =>
        val key: (Location, Int) = location -> cluster.min

        val size = cluster.size
        val radius = (cluster.max - cluster.min) / 2.0 + 1 // avoid radius = 0
        val densityScore = clamp(size / radius, 0, 5) / 5.0

        val sizeScore = clamp(size, 0, 20) / 20.0

        val finalScore =.6 * densityScore +.4 * sizeScore

        SearchResultEntry(location.owner, location.repo, location.file, cluster.min, finalScore.toFloat)
      }
    }
  }
}



case class Location(owner: String, repo: String, file: String)
case class FeatureEntry(loc: Location, feature: String, line: Int)
