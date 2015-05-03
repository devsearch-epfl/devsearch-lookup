package devsearch.lookup

import reactivemongo.api.MongoDriver

/**
 * Interract with the db to fetch files and line for a query
 */
object FeatureDB {

  val driver = new MongoDriver
  val connection = driver.connection(List("localhost"))
  val db = connection("devsearch")
  val featureCollection = db("features")

  /**
   * fetches matches from the DB
   * @param features a list of feature index
   * @return A stream of ("owner/repo/path/to/file", List((featureIndex, lineNb)))
   */
  def getMatchesFromDb(features: Seq[String]): Stream[(String, List[(String, Long)])] = ???
}
