package devsearch.lookup

import java.io.{PrintStream, ByteArrayOutputStream}

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
    case SearchRequest(features, lang) =>
      log.info("PartitionLookup: receive SearchRequest")

      getFeaturesAndScores(features, lang) pipeTo sender
    case x => log.error(s"Received unexpected message $x")
  }

  def getFeaturesAndScores(features: Set[String], lang: Seq[String]): Future[SearchResult] = {

    FeatureDB.getMatchesFromDb(features, lang).map {
      docHitsStream =>
      val (results, count) = FindNBest[SearchResultEntry](docHitsStream.flatMap(getScores(_, features.size)), _.score, 10)
      SearchResultSuccess(results.toSeq, count)
    }.recover({
      case e =>
        val baos = new ByteArrayOutputStream()
        val ps = new PrintStream(baos)
        e.printStackTrace(ps)
        ps.flush()
        SearchResultError(baos.toString("UTF-8"))
    })
  }

  def clamp(x: Double, min: Double, max: Double): Double = if (x < min) min else if (x > max) max else x

  def getScores(entry: DocumentHits, nbQueryFeatures: Int): Iterable[SearchResultEntry] = entry match {


    case DocumentHits(location, streamOfHits) => {

      //      val scoreFuture = RankingDB.getRanking(location)
      //      val score = Await.result(scoreFuture, Duration("100ms"))

      // TODO: Cluster epsilon should maybe depend on the language of the file?
      //       Typically scala features will be much closer to each other than in Java...

      val positions = streamOfHits.map(_.line).toArray
      val clusters = DBSCAN(positions, 5.0, positions.length min 3)

//      val featuresByLine = streamOfHits map (h => (h.line -> h.feature))
      val featuresByLine = streamOfHits.groupBy(_.line)

      clusters.map { cluster =>

        val size = cluster.size
        val radius = (cluster.max - cluster.min) / 2.0 + 1 // avoid radius = 0
        val densityScore = clamp(size / radius, 0, 5) / 5.0

        val sizeScore = clamp(size, 0, 20) / 20.0

        //get all the different features of this cluster and devide it by nbQueryFeatures
//        val featureCount = featuresByLine.foldLeft(collection.mutable.Map[String, Int]()) ((map, curr) => {
//          map + (curr._2 -> (map.getOrElse(curr._2, 0) + 1))
//        })

        val distinctFeatures = cluster.flatMap(line => featuresByLine(line).map(_.feature))

        val ratioOfMatches = distinctFeatures.size.toDouble/nbQueryFeatures

        val finalScore =.6 * densityScore +.3 * sizeScore + .1 * ratioOfMatches

        val scoreBreakdown = Map("final" -> finalScore, "density" -> densityScore, "size" -> sizeScore, "ratioOfMatches" -> ratioOfMatches)

        //        val finalScore =.4 * densityScore +.3 * sizeScore + 0.3 * score

        SearchResultEntry(location.owner, location.repo, location.file, cluster.min, cluster.max, finalScore.toFloat, scoreBreakdown, distinctFeatures)
      }
    }
  }
}

case class Location(owner: String, repo: String, file: String)
case class FeatureEntry(loc: Location, feature: String, line: Int)
