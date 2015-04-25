package devsearch

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._

/**
 * Responsible for sorting all the matching files by different criteria
 */
object MatchSorter {

  def sort(groupedFeatures: RDD[(Location, Iterable[FeatureData])], withRanking: Boolean = true, numToReturn: Int = 100)(implicit sc: SparkContext): Array[(Location, Int)] = {
    val repoRanking: RDD[(String, Double)] = if (!withRanking) sc.emptyRDD else {
      val reposRDD = (if (!withRanking) sc.emptyRDD else sc.textFile(Config.repoRankPath))
      val parsedRanking = reposRDD.flatMap(l => l.split(",") match {
        case Array(key, value) => List(key -> value.toDouble)
        case _ => Nil
      })

      // normalize repo-rank results
      val (minRanking, maxRanking) = parsedRanking.map(p => p._2 -> p._2).reduce((p1, p2) => (p1._1 min p2._1, p1._2 max p2._2))
      parsedRanking.map(p => p._1 -> (p._2 - minRanking) / (maxRanking - minRanking))
    }

    def clamp(x: Double, min: Double, max: Double): Double = if (x < min) min else if (x > max) max else x

    NBestFinder.getNBestMatches(numToReturn, groupedFeatures.map(p => p._1.repository -> (p._1.path, p._2))
      .leftOuterJoin(repoRanking).flatMap { case (repository, ((path, features), rankingScoreOpt)) =>
        val location = Location(repository, path)
        val rankingScore = rankingScoreOpt getOrElse .0
        
        // TODO: Cluster epsilon should maybe depend on the language of the file?
        //       Typically scala features will be much closer to each other than in Java...
        val positions = features.map(_.line).toArray
        val clusters = DBSCAN(positions, 5.0, positions.length min 3)

        clusters.map { cluster =>
          val key = location -> cluster.min

          val size = cluster.size
          val radius = (cluster.max - cluster.min) / 2.0 + 1 // avoid radius = 0
          val densityScore = clamp(size / radius, 0, 5) / 5.0

          val sizeScore = clamp(size, 0, 20) / 20.0

          val finalScore = .3 * densityScore + .2 * sizeScore + .5 * rankingScore

          key -> finalScore
        }
      }).map(_._1)
  }
}
