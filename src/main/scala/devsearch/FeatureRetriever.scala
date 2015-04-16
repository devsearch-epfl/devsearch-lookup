package devsearch
import org.apache.spark.rdd._

/**
 * Responsible for finding all the features that match the key
 */
object FeatureRetriever {
  private val featuresPath = "features"
  def get(keys: Seq[String]): RDD[FeatureData] = {
    // load in the features file(s) and find all matching features
    ???
  }
}
