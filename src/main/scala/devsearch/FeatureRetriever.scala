package devsearch
import org.apache.spark.rdd._

case class FeatureData(key: String, user: String, repo: String, dir: String, file: String, line: Int)

/**
 * Responsible for finding all the features that match the key
 */
object FeatureRetriever {
  def get(keys: Seq[String]): RDD[FeatureData] = {
    // load in the features file(s) from Config.featuresPath and find all matching features
    ???
  }
}
