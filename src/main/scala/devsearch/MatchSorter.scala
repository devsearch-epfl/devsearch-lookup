package devsearch

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

abstract class MatchSorter {
  protected def clamp(x: Double, min: Double, max: Double): Double = if (x < min) min else if (x > max) max else x

  protected def cluster(features: Iterable[FeatureData]): Iterable[Set[Int]] = {
    // TODO: Cluster epsilon should maybe depend on the language of the file?
    //       Typically scala features will be much closer to each other than in Java...
    val positions = features.map(_.line).toArray
    val clusters = DBSCAN(positions, 5.0, positions.length min 3)
    clusters
  }

  def sort(groupedFeatures: RDD[(Location, Iterable[FeatureData])], withRanking: Boolean = true, numToReturn: Int = 100)(implicit sc: SparkContext): Array[(Location, Int)]
}
