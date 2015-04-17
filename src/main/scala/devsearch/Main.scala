package devsearch

import org.apache.spark._

import org.apache.log4j.Logger
import org.apache.log4j.Level

object Main {
  def main(args: Array[String]) {
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)


    val conf = new SparkConf().setAppName("DevSearch Lookup")
      .set("spark.ui.port", "4781")

    implicit val spark = new SparkContext(conf)


    if (args.isEmpty) {
      println("Missing feature key arguments")
      sys.exit(1)
    }
    val keys = args

    val matchingFeatures = FeatureRetriever.get(keys)
    val matchingFeaturesByFile = matchingFeatures.groupBy { f =>
      Location(f.user + "/" + f.repo, f.path)
    }

//    println("\n\n\n\nMatchin finished, found " + matchingFeatures.count() + " feature matches for " + matchingFeaturesByFile.count() + "\n\n\n\n")

//    val results = MatchSorter.sort(matchingFeaturesByFile).take(Config.maxNumResults
    val results = SimpleMatchSorter.sort(matchingFeaturesByFile).take(Config.maxNumResults)

//    println("\n\n\n\nResult   START ")
    results.foreach(item => println("Result: " + item._1 + " - " + item._2))
//    println("Result   END\n\n\n\n")

    spark.stop()
  }
}
