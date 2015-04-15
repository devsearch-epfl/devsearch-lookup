package devsearch
import org.apache.spark.rdd._

/**
 * Responsible for sorting all the matching files by different criteria
 */
object MatchSorter {
  def sort[A](groupedFeatures: RDD[(A, Iterable[FeatureData])]): RDD[A] = {
    // load in repo rank
    // compute density of features
    // sort by number of matches, density, diversity, repo rank, ...
    ???
  }
}
