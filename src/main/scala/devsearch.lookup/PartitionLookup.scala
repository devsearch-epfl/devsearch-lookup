package devsearch.lookup

import java.io.{PrintStream, ByteArrayOutputStream}

import akka.actor._
import akka.pattern.pipe
import devsearch.parsers.Languages
import devsearch.features._

import scala.concurrent.Future

/**
 * This actor is permanently responsible for one partition of the database. It
 * receives requests and returns the K best matches of the partition.
 */
class PartitionLookup() extends Actor with ActorLogging {

  import context.dispatcher

  log.info("Starting PartitionLookup")

  val STAGE_1_LIMIT: Long = 10000
  val noCountDefaultValue: Long = 0
  val REPO_RANK_LOG_MAX: Double = 4.5

  override def receive = {
    case SearchRequest(features, lang, start, len) =>
      log.info("PartitionLookup: receive SearchRequest starting local partition lookup (millis=" + System.currentTimeMillis + ")")

      getFeaturesAndScores(features.map(_.key), lang, len, start) pipeTo sender

    case x => log.error(s"Received unexpected message $x")
  }

  def getFeaturesAndScores(features: Set[String], languages: Set[String], len: Int, from: Int): Future[SearchResult] = {
    if (features.isEmpty) return Future(SearchResultError("feature set is empty"))

    for {
      localFeatureLangOccs <- TimedFuture(FeatureDB.getFeatureOccurrenceCount(FeatureDB.LOCAL_OCCURENCES_COLLECTION_NAME, features, Set()), name = "local occs")
      globalFeatureLangOccs <- TimedFuture(FeatureDB.getFeatureOccurrenceCount(FeatureDB.GLOBAL_OCCURENCES_COLLECTION_NAME, features, Set()), name = "global occs")
      matches <- {

        // get count of features that match this request locally
        val localFeatureOccs = localFeatureLangOccs.groupBy(_._1._1).mapValues(_.foldLeft(0L)((x,entry) => x + entry._2))

        // sort features in order of increasing count (in the local database)
        val sortedFeatures = features.toList.sortBy(f => localFeatureOccs.get(f).getOrElse(noCountDefaultValue))

        // smallest feature must be rare even if there are too many occurrences because `rareFeatures` must be nonempty
        var resCount: Long = 0L
        var rareFeatures: Set[String] = Set()
        var commonFeatures: Set[String] = Set()
        log.info(s" Sorted features $sortedFeatures")
        log.info(s" Local counts $localFeatureOccs")

        for (feature <- sortedFeatures) {
          if (resCount + localFeatureOccs.get(feature).getOrElse(0L) <= STAGE_1_LIMIT) {
            resCount += localFeatureOccs.get(feature).getOrElse(0L)
            rareFeatures += feature
          } else {
            commonFeatures += feature
          }
        }
        log.info(s"Common features : $commonFeatures")
        log.info(s"Rare features : $rareFeatures")
        FeatureDB.getMatchesFromDb(rareFeatures, commonFeatures, languages).map {
          docHitsStream =>

            // from the stream of documents
            val (results, count) = FindNBest[SearchResultEntry](docHitsStream.flatMap(getScores(_, features, globalFeatureLangOccs)), _.score, from+len)

            println("done searching db! (millis=" + System.currentTimeMillis + ")")
            SearchResultSuccess(results.drop(from).toSeq, count)

        }.recover({

          // in case of error send the stack trace to the merger
          case e =>
            val baos = new ByteArrayOutputStream()
            val ps = new PrintStream(baos)
            e.printStackTrace(ps)
            ps.flush()
            SearchResultError(baos.toString("UTF-8"))
        })
      }
    } yield matches
  }

  def clamp(x: Double, min: Double, max: Double): Double = if (x < min) min else if (x > max) max else x

  def rarityWeightFunction(x: Long): Double = 1/(1+Math.exp((Math.sqrt(x)-20)/10))

  def getScores(entry: DocumentHits, features: Set[String], featureLangOccs: Map[(String,String), Long]): Iterable[SearchResultEntry] = entry match {

    case DocumentHits(location, repoRank, streamOfHits) => {

      val language = Languages.guess(location.file).getOrElse("")

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

        /** TODO: reconstruct the feature correctly */
        val features = cluster.flatMap(line => featuresByLine(line)).map(e =>
            new Feature(CodePiecePosition(CodeFileLocation("dummy_user", "dummy_repo", "dummy_file"), e.line)) {
              def key = e.feature
              override def toString = "Feature(" + key + ")"
            }
        )

        val distinctFeatures = features.map(_.key)

        val ratioOfMatches = distinctFeatures.size.toDouble/features.size

        val rarityScore = distinctFeatures.map(feature => rarityWeightFunction(featureLangOccs.getOrElse((feature, language), 1L))).sum

        val repoRankScore = Math.log(repoRank)/Math.log(25566)

        var finalScore =.6 * densityScore + .4 * sizeScore + .3 * rarityScore + .1 * ratioOfMatches + .4 * repoRankScore

        if (distinctFeatures.contains("map call"))
          finalScore = finalScore + 100

        val scoreBreakdown = Map("final" ->  finalScore, "density" -> .6 * densityScore, "size" -> .4 * sizeScore, "rarity" -> .3 * rarityScore, "ratioOfMatches" -> .1 * ratioOfMatches, "repoRank" -> .4 * repoRankScore)

        SearchResultEntry(location.owner, location.repo, location.file, cluster.min, cluster.max, finalScore.toFloat, scoreBreakdown, features)
      }
    }
  }
}

case class Location(owner: String, repo: String, file: String)
case class FeatureEntry(loc: Location, feature: String, line: Int)
