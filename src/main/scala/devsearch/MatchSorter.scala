package devsearch

import org.apache.spark._
import org.apache.spark.rdd._

/**
 * Responsible for sorting all the matching files by different criteria
 */
object MatchSorter {

  def sort(groupedFeatures: RDD[(Location, Iterable[FeatureData])])(implicit sc: SparkContext): RDD[(Location, Int)] = {
    val repoRanking = sc.textFile("hdfs:///projects/devsearch/ranking/*").flatMap(l => l.split(",") match {
      case Array(key, value) => List(key -> value.toDouble)
      case _ => Nil
    })

    groupedFeatures.map(p => p._1.repository -> (p._1.path, p._2))
      .leftOuterJoin(repoRanking).flatMap { case (repository, ((path, features), rankingScoreOpt)) =>
        val location = Location(repository, path)
        val rankingScore = rankingScoreOpt getOrElse .0
        
        // TODO: Cluster epsilon should maybe depend on the language of the file?
        //       Typically scala features will be much closer to each other than in Java...
        val clusters = DBSCAN(features.map(_.line).toArray, 5.0, 3)

        clusters.map { cluster =>
          val key = location -> cluster.min

          val size = cluster.size
          val radius = (cluster.max - cluster.min) / 2.0 + 1 // avoid radius = 0
          val densityScore = size / radius

          // TODO: extend scoring function
          val finalScore = densityScore * rankingScore

          key -> finalScore
        }
      }.sortBy(_._2).map(_._1)
  }
}
