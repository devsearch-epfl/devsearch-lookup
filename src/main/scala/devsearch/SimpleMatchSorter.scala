package devsearch

import org.apache.spark._
import org.apache.spark.rdd._

/**
 * Responsible for sorting all the matching files by different criteria
 */
object SimpleMatchSorter {

  def sort(groupedFeatures: RDD[(Location, Iterable[FeatureData])], withRanking: Boolean = true)(implicit sc: SparkContext): RDD[(Location, Int)] = {

    // sorts by number of feature per location
    groupedFeatures.sortBy(_._2.size).map{
      case (location, features) => (location, features.head.line);
    }

  }
}
