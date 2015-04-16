package devsearch
import org.apache.spark.rdd._
import scala.reflect.ClassTag

/**
 * Responsible for sorting all the matching files by different criteria
 */
object MatchSorter {
  def sort[A: ClassTag](groupedFeatures: RDD[(A, Iterable[FeatureData])]): RDD[A] = {
    // load in repo rank data from Config.repoRankPath
    // compute density and diversity of features
    // sort by number of matches, density, diversity, repo rank, ...
    groupedFeatures.map(x => x._1)
  }
}
