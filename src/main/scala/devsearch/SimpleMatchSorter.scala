package devsearch

import org.apache.spark._
import org.apache.spark.rdd._

/**
 * Responsible for sorting all the matching files by different criteria
 */
object SimpleMatchSorter extends MatchSorter {

  def sort(groupedFeatures: RDD[(Location, Iterable[FeatureData])], withRanking: Boolean = true, numToReturn: Int = 100)(implicit sc: SparkContext): Array[(Location, Int)] = {

    // sorts by number of feature per location and returns numToReturn best matches
    NBestFinder.getNBestMatches(numToReturn, groupedFeatures.flatMap{
      case (location, features) =>

        val clusters = cluster(features)

        clusters.map { cluster =>
          val key:(Location, Int) = location -> cluster.min

          val size = cluster.size
          val radius = (cluster.max - cluster.min) / 2.0 + 1 // avoid radius = 0
          val densityScore = clamp(size / radius, 0, 5) / 5.0

          val sizeScore = clamp(size, 0, 20) / 20.0

          val finalScore = .6 * densityScore + .4 * sizeScore

          key -> finalScore

        }
    }).map(_._1)

  }
}
