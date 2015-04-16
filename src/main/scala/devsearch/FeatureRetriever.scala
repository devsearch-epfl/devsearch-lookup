package devsearch

import org.apache.spark._
import org.apache.spark.rdd._

import scala.util.parsing.combinator.RegexParsers

case class Location(repository: String, path: String)

case class FeatureData(key: String, user: String, repo: String, path: String, line: Int)

/**
 * Parses a line of the feature database into a feature data
 */
object FeatureIndexParser extends RegexParsers {

  def featureEntry: Parser[FeatureData] = value~separator~value~separator~value~separator~value~separator~number ^^ {
    case featKey~_~user~_~repo~_~file~_~lineNb => FeatureData(featKey, user, repo, file, lineNb.toInt)
  }

  val value: Parser[String] = "(\\\\,|[^,])*".r
  val number: Parser[String] = "\\d*".r
  val separator: Parser[String] = ",".r
}

/**
 * Responsible for finding all the features that match the key
 */
object FeatureRetriever {
  def get(keys: Seq[String]): RDD[FeatureData] = {

    val sc = new SparkContext()
    val featureIndex: RDD[FeatureData] = sc.textFile(Config.featuresPath).map( (line) => FeatureIndexParser
      .parse(FeatureIndexParser.featureEntry, line) )
      .collect{ case e if !e.isEmpty => e.get}

    return featureIndex.filter(feature => keys.contains(feature.key))
  }
}
