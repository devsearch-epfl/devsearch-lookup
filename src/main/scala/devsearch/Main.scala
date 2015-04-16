package devsearch

import org.apache.spark._
import org.apache.spark.rdd._

case class FeatureData(key: String, user: String, repo: String, dir: String, file: String, line: Int)

object Main {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("DevSearch Lookup")
                              .setMaster("local")
    val spark = new SparkContext(conf)

    val maxNumResults = 10

    if (args.isEmpty) {
      println("Missing feature key arguments")
      sys.exit(1)
    }
    val keys = args

    val matchingFeatures = FeatureRetriever.get(keys)
    val matchingFeaturesByFile = matchingFeatures.groupBy(f => (f.user, f.repo, f.dir, f.file))
    val results = MatchSorter.sort(matchingFeaturesByFile).take(maxNumResults)

    println(results)
    spark.stop()
  }
}
