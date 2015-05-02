package devsearch.utils

import org.apache.spark.{rdd, SparkConf, SparkContext}
import devsearch.features.Feature
import spray.json._
import spray.json.DefaultJsonProtocol


/**
 * These two classes are needed for transforming RepoRanks and Features into JSON format. (THX Pwalch!)
 */
case class JsonLine(line: String)
object LineJsonProtocol extends DefaultJsonProtocol {
  implicit val jsonNumberFormat = jsonFormat(JsonLine, "$numberInt")
}
case class JsonFeature(key: String, ownerRepo: String, fileName: String, line: JsObject)
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
 * The dataPreparer is needed for converting to JSON and distributing our data (features, RepoRank) to several buckets.
 * It basically transforms the python scripts features2json.py and distributeFiles.py into a spark job.
 *
 * The buckets are chosen by hashing "owner/repo" of the features and ranks. Each of the Akka PartitionLookup nodes will
 * be responsible for one of those buckets.
 *
 * This spark job applies 'consistent hashing' to our features. More about consistent hashing on
 * http://www.tom-e-white.com//2007/11/consistent-hashing.html.
 * The algorithm used in our case is the one of JosephMoniz: http://blog.plasmaconduit.com/consistent-hashing/ and
 * https://github.com/JosephMoniz/scala-hash-ring and
 *
 *
 * Usage:
 * - 1st argument is the input directory containing the feature files
 * - 2nd argument is the input directory containing the repoRank files
 * - 3rd argument is the output directory of the buckets
 * - 4th argument is the number of buckets
 */
object DataPreparer {


  def main(args: Array[String]) {

    //some argument checking...
    if(args.length != 4)
      throw new ArgumentException("You need to enter 4 arguemnts, not " + args.length + ". ")
    if(!args(3).matches("""\d+"""))
      throw new ArgumentException("4th argument must be an integer.")

    val featureInput  = args(0)
    val repoRankInput = args(1)
    val outputPath    = args(2)
    val nbBuckets     = args(3).toInt



    val sparkConf = new SparkConf().setAppName("Data Splitter").setMaster("local[4]")
    val sc = new SparkContext(sparkConf)


    //prepare consistent hashing...
    //bucket0 always seems to be empty...
    val buckets = (0 to nbBuckets).tail.map(nb => HashRingNode("bucket"+nb, 100))
    val ring = new SerializableHashRing(buckets)


    //read files
    val features = sc.textFile(featureInput)
    val ranks    = sc.textFile(repoRankInput)


    //transform into JSON and assign each line to a bucket.
    //The bucket is chosen according to ownerRepo.
    val featuresJSON = features.map(Feature.parse(_)).map(f => {
      import FeatureJsonProtocol._
      import LineJsonProtocol._
      val ownerRepo = f.pos.location.user+"/"+f.pos.location.repoName
      val jsonFeature = JsonFeature(f.key,
        ownerRepo,
        f.pos.location.fileName,
        JsonLine(f.pos.line.toString).toJson.asJsObject).toJson.asJsObject.toString
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


    //save the output
    for (i <- 0 to nbBuckets) {
      featuresJSON.filter(_._1 == "bucket" + i).map(_._2).saveAsTextFile(outputPath + "/features/bucket" + i)
      ranksJSON.filter(_._1 == "bucket" + i).map(_._2).saveAsTextFile(outputPath + "/repoRank/bucket" + i)
    }
  }
}


//Thrown when arguments are somehow incorrect...
case class ArgumentException(cause:String)  extends Exception("ERROR: " + cause + """
    |        Correct usage:
    |         - arg1 = path/to/features
    |         - arg2 = path/to/repoRank
    |         - arg4 = path/to/bucket/output
    |         - arg3 = nbBuckets (Integer)
  """.stripMargin)
