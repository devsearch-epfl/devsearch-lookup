package devsearch.lookup

import akka.actor._
import akka.pattern.pipe

import scala.concurrent.Future

/**
 * This actor is permanently responsible for one partition of the database. It
 * receives requests and returns the K best matches of the partition.
 */
class PartitionLookup() extends Actor with ActorLogging {

  import context.dispatcher

  log.info("Starting PartitionLookup")

  override def receive = {
    case SearchRequest(features) =>
      log.info("PartitionLookup: receive SearchRequest")

      getFeaturesAndScores(features) pipeTo sender
    case x => log.error(s"Received unexpected message $x")
  }

  def getFeaturesAndScores(features: Seq[String]): Future[SearchResult] = {
    FeatureDB.getMatchesFromDb(features).map(
      docHitsStream => SearchResultSuccess(
        FindNBestNew[SearchResultEntry](docHitsStream.flatMap(getScores), _.score, 10).toSeq)
    ).recover({
      case e => SearchResultError(e.getMessage)
    })
  }


//  /**
//   * Generates superfeatures of each file and returns a searchResultEntry including score.
//   * @param entry
//   * @return
//   */
//  def getScore(entry: (Location, List[FeatureEntry])): SearchResultEntry = entry match{
//    case (Location(owner, repo, file), list) => {
//
//      val nbMatches = list.length
//
//      val nbDifferentFeatures = list.groupBy(_.feature).toList.length
//
//
//      //TODO: What kind of metric should we use here? we might need filesize for normalizing...
//      //standard deviation is a bad choice!
//      val lineAvg = list.map(_.line).sum/list.size
//      val stdDevLines = Math.sqrt((list.map(e => Math.pow(e.line - lineAvg, 2)).sum)/(list.size - 1))
//
//
//      //TODO: load repoRank into Mongo, query it or this file.
//      val repoRank = 0
//
//      //TODO: assign good weights!
//      val score = 5 * nbMatches + 2 * nbDifferentFeatures - stdDevLines + repoRank
//
//
//
//      print("\n\t"+file+" score: "+score+" ("+nbMatches+", "+nbDifferentFeatures+", "+10/stdDevLines+")\n\nS")
//
//
//
//      SearchResultEntry(owner, repo, file, 0, score.toFloat)
//
//    }
//  }

  def clamp(x: Double, min: Double, max: Double): Double = if (x < min) min else if (x > max) max else x

  def getScores(entry: DocumentHits): Iterable[SearchResultEntry] = entry match {


    case DocumentHits(location, streamOfHits) => {

//      val scoreFuture = RankingDB.getRanking(location)
//      val score = Await.result(scoreFuture, Duration("100ms"))

      // TODO: Cluster epsilon should maybe depend on the language of the file?
      //       Typically scala features will be much closer to each other than in Java...

      val positions = streamOfHits.map(_.line.toInt).toArray
      val clusters = DBSCAN(positions, 5.0, positions.length min 3)

      clusters.map { cluster =>

        val size = cluster.size
        val radius = (cluster.max - cluster.min) / 2.0 + 1 // avoid radius = 0
        val densityScore = clamp(size / radius, 0, 5) / 5.0

        val sizeScore = clamp(size, 0, 20) / 20.0

        val finalScore =.6 * densityScore +.4 * sizeScore

//        val finalScore =.4 * densityScore +.3 * sizeScore + 0.3 * score

        SearchResultEntry(location.owner, location.repo, location.file, cluster.min, finalScore.toFloat)
      }
    }
  }
}

case class Location(owner: String, repo: String, file: String)
case class FeatureEntry(loc: Location, feature: String, line: Int)
