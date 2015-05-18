package devsearch.lookup

import java.io.{PrintStream, ByteArrayOutputStream}

import akka.actor._
import akka.pattern.pipe
import devsearch.parsers.Languages
import devsearch.features.Feature

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

  override def receive = {
    case SearchRequest(features, lang, start, len) =>
      log.info("PartitionLookup: receive SearchRequest")

      getFeaturesAndScores(features.map(_.key), lang.map(_.toLowerCase)) pipeTo sender
    case x => log.error(s"Received unexpected message $x")
  }

  def getFeaturesAndScores(features: Set[String], languages: Set[String]): Future[SearchResult] = {
    if (features.isEmpty) return Future(SearchResultError("feature set is empty"))

    for {
      localFeatureLangOccs <- FeatureDB.getFeatureOccurrenceCount(FeatureDB.LOCAL_OCCURENCES_COLLECTION_NAME, features, languages)
      globalFeatureLangOccs <- FeatureDB.getFeatureOccurrenceCount(FeatureDB.GLOBAL_OCCURENCES_COLLECTION_NAME, features, languages)
      matches <- {
        val localFeatureOccs = localFeatureLangOccs.groupBy(_._1._1).mapValues(_.foldLeft(0L)((x,entry) => x + entry._2))

        val sortedFeatures = features.toList.sortBy(f => localFeatureOccs.get(f).getOrElse(noCountDefaultValue))

        // smallest feature must be rare even if there are too many occurrences because `rareFeatures` must be nonempty
        var resCount: Long = 0L
        var rareFeatures: Set[String] = Set()
        var commonFeatures: Set[String] = Set()

        for (feature <- sortedFeatures) {
          if (resCount + localFeatureOccs.getOrElse(feature, 0L) <= STAGE_1_LIMIT) {
            resCount += localFeatureOccs.getOrElse(feature, 0L)
            rareFeatures += feature
          } else {
            commonFeatures += feature
          }
        }

        FeatureDB.getMatchesFromDb(rareFeatures, commonFeatures, languages).map {
          docHitsStream =>
          val (results, count) = FindNBest[SearchResultEntry](docHitsStream.flatMap(getScores(_, features, globalFeatureLangOccs)), _.score, 10)
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
    } yield matches
  }

  def clamp(x: Double, min: Double, max: Double): Double = if (x < min) min else if (x > max) max else x

  def rarityWeightFunction(x: Long): Double = 1/(1+Math.exp((Math.sqrt(x)-20)/10))

  def getScores(entry: DocumentHits, features: Set[String], featureLangOccs: Map[(String,String), Long]): Iterable[SearchResultEntry] = entry match {

    case DocumentHits(location, streamOfHits) => {

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

        val distinctFeatures = cluster.flatMap(line => featuresByLine(line).map(e => Feature.parse(s"${e.feature},owner/repo/file.java,${e.line}")))

        val ratioOfMatches = distinctFeatures.size.toDouble/features.size

        val rarityScore = distinctFeatures.map(feature => rarityWeightFunction(featureLangOccs.getOrElse((feature.key, language), 1L))).sum

        val finalScore =.6 * densityScore +.3 * sizeScore + .4 * rarityScore + .1 * ratioOfMatches

        val scoreBreakdown = Map("final" -> finalScore, "density" -> densityScore, "size" -> sizeScore, "rarity" -> rarityScore, "ratioOfMatches" -> ratioOfMatches)

        SearchResultEntry(location.owner, location.repo, location.file, cluster.min, cluster.max, finalScore.toFloat, scoreBreakdown, distinctFeatures)
      }
    }
  }
}

case class Location(owner: String, repo: String, file: String)
case class FeatureEntry(loc: Location, feature: String, line: Int)
