package devsearch.utils

import com.plasmaconduit.distributed._
import org.apache.spark.{rdd, SparkConf, SparkContext}
import scala.util.parsing.json.JSON
import devsearch.features.Feature
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol



case class JsonFeature(key: String, ownerRepo: String, fileName: String, line: Int)
object FeatureJsonProtocol extends DefaultJsonProtocol {
  implicit val jsonFeatureFormat = jsonFormat4(JsonFeature)
}

case class JsonRepoRank(ownerRepo: String, score: Double)
object RepoRankJsonProtocol extends DefaultJsonProtocol {
  implicit val jsonRepoRankFormat = jsonFormat2(JsonRepoRank)
}


/**
 * Created by hubi on 5/1/15.
 *
 * The dataSplitter is needed for converting to JSON and distributing our data (features, RepoRank) to several buckets.
 * Each of the Akka PartitionLookup nodes will be responsible for one of those buckets.
 *
 * This spark job applies 'consistent hashing' to our features. More about consistent hashing on
 * http://www.tom-e-white.com//2007/11/consistent-hashing.html
 *
 *
 * Usage:
 * - first argument is the input json file containing the features
 * - the other arguments are at least one path or tuple: bucket_x.json weight_x (weight_x is optional)
 */
object DataSplitter {

  //val featureInput  = "/home/hubi/Documents/BigData/DevSearch/bla/part-00000"
  //val repoRankInput = "/home/hubi/Documents/BigData/DevSearch/testRanking/*"
  //val outputPath    = "/home/hubi/Documents/BigData/DevSearch/buckets"
  //val nbBuckets     = 5



  def main(args: Array[String]) {

    //some argument checking...
    if(args.length != 4) throw new ArgumentException("You need to enter 4 arguemnts, not " + args.length + ". ")
    if(!args(3).matches("""\d+""")) throw new ArgumentException("4th argument must be an integer.")

    val featureInput  = args(0)
    val repoRankInput = args(1)
    val outputPath    = args(2)
    val nbBuckets     = args(3).toInt



    val sparkConf = new SparkConf().setAppName("Data Splitter").setMaster("local[4]")
    val sc = new SparkContext(sparkConf)


    //prepare consistent hashing...
    val buckets = (1 to nbBuckets).tail.map(nb => HashRingNode("bucket"+nb, 100))
    val ring = new HashRing(buckets)


    //read files
    val features = sc.textFile(featureInput)
    val ranks    = sc.textFile(repoRankInput)


    //transform into JSON and split the lines
    val featuresJSON = features.map(Feature.parse(_)).map(f => {
      import FeatureJsonProtocol._
      val ownerRepo = f.pos.location.user+"/"+f.pos.location.repoName
      val jsonFeature = JsonFeature(f.key,
        ownerRepo,
        f.pos.location.fileName,
        f.pos.line).toJson.asJsObject.toString
      ring.get(ownerRepo) match {
        case Some(bucket: String) => (bucket, jsonFeature)
      }
    })

    val ranksJSON = ranks.map{
      r =>
        val splitted = r.split(",")
        import RepoRankJsonProtocol._
        val jsonRepoRank = JsonRepoRank(splitted(0), splitted(1).toDouble).toJson.asJsObject.toString
        ring.get(splitted(0)) match {
          case Some(bucket: String) => (bucket, jsonRepoRank)
        }
    }



    //println("\n\n\n\n\n"+featuresJSON.collect())


    //assign buckets
    //val featuresAssignedBuckets = splitData(features, ring)
    //val ranksAssignedBuckets    = splitData(ranks, ring)


    //create files
    for (i <- 1 to nbBuckets) {
      featuresJSON.filter(_._1 == "bucket" + i).map(_._2).saveAsTextFile(outputPath + "/features/bucket" + i)
      ranksJSON.filter(_._1 == "bucket" + i).map(_._2).saveAsTextFile(outputPath + "/repoRank/bucket" + i)
    }
  }
}


case class ArgumentException(cause:String)  extends Exception("ERROR: " + cause + """
    |        Correct usage:
    |         - arg1 = path/to/features
    |         - arg2 = path/to/repoRank
    |         - arg3 = nbBuckets (Integer)
    |         - arg4 = path/to/bucket/output
  """.stripMargin)
