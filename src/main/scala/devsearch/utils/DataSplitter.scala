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

  val featureInput  = "/home/hubi/Documents/BigData/DevSearch/bla/part-00000"
  val repoRankInput = "/home/hubi/Documents/BigData/DevSearch/testRanking/*"
  val outputPath    = "/home/hubi/Documents/BigData/DevSearch/buckets"
  val nbBuckets     = 5


  // throws each json line into a bucket (according to owner/repo)
  def splitData(jsonLines: RDD[String], ring: HashRing): rdd.RDD[(String, String)] = {
    jsonLines map {
      json => JSON.parseFull(json) match {
        case Some(m: Map[String, Any]) => {
          m("ownerRepo") match {
            case s:String => ring.get(s) match {case Some(b: String) => (b, json)}
          }
        }
      }
    }
  }


  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("Data Splitter").setMaster("local[4]")
    val sc = new SparkContext(sparkConf)


    //prepare consistent hashing...
    val buckets = (0 to nbBuckets).tail.map(nb => HashRingNode("bucket"+nb, 100))
    val ring = new HashRing(buckets)


    //read files
    val features = sc.textFile(featureInput)
    val ranks    = sc.textFile(repoRankInput)


    //transform into JSON
    /*val featuresJSON = features.map(Feature.parse(_)).map(f => {
      import FeatureJsonProtocol._
      JsonFeature(f.key,
        f.pos.location.user+"/"+f.pos.location.repoName,
        f.pos.location.fileName,
        f.pos.line).toJson.asJsObject.toString

    })*/
    val ranksAssignedBuckets = ranks.map{
      r =>
        val splitted = r.split(",")
        import RepoRankJsonProtocol._
        val json = JsonRepoRank(splitted(0), splitted(1).toDouble).toJson.asJsObject.toString
        ring.get(splitted(0)) match {
          case Some(bucket: String) => (bucket, json)
        }
    }





    //println("\n\n\n\n\n"+featuresJSON.collect())


    //assign buckets
    //val featuresAssignedBuckets = splitData(features, ring)
    //val ranksAssignedBuckets    = splitData(ranks, ring)


    //create files
    for (i <- 1 to nbBuckets) {
      //featuresAssignedBuckets.filter(_._1 == "bucket" + i).map(_._2).saveAsTextFile(outputPath + "/features/bucket" + i)
      ranksAssignedBuckets.filter(_._1 == "bucket" + i).map(_._2).saveAsTextFile(outputPath + "/repoRank/bucket" + i)
    }
  }
}
