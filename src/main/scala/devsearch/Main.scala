package devsearch

import org.apache.spark._
import org.apache.spark.rdd._

object Main {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("DevSearch Lookup")
                              .setMaster("local")

    implicit val spark = new SparkContext(conf)

    if (args.isEmpty) {
      println("Missing feature key arguments")
      sys.exit(1)
    }
    val keys = args

    val matchingFeatures = FeatureRetriever.get(keys)
    val matchingFeaturesByFile = matchingFeatures.groupBy { f =>
      Location(f.user + "/" + f.repo, f.dir + "/" + f.file)
    }

    val results = MatchSorter.sort(matchingFeaturesByFile).take(Config.maxNumResults)

    println(results)
    spark.stop()
  }
}
