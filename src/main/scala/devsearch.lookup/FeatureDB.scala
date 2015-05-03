package devsearch.lookup

import reactivemongo.api.MongoDriver
import reactivemongo.bson.{BSONArray, BSONDocument}

import scala.concurrent.Future

/**
 * Interract with the db to fetch files and line for a query
 */
object FeatureDB {

  val DB_SERVER = "localhost"
  val DB_NAME = "devsearch"
  val FEATURE_COLLECTION_NAME = "features"

  val driver = new MongoDriver
  val connection = driver.connection(List(DB_SERVER))
  val db = connection(DB_NAME)
  val featureCollection = db(FEATURE_COLLECTION_NAME)

  /**
   * fetches matches from the DB
   * @param features a list of feature index
   * @return A stream of ("owner/repo/path/to/file", List((featureIndex, lineNb)))
   */
  def getMatchesFromDb(features: Seq[String]): Future[List[(Location, List[(Long, String)])]] = {

    val query = BSONDocument("$or" -> BSONArray(
      features.map(f => BSONDocument("feature" -> f))
    ))

    /*
      Performs an aggregation on the db to fetch each matched files with a list of lineNb and featurename
      Example in the mongo shell:
      db.features.aggregate([ {$match: { feature: { $in: ["className=ExampleModule", "typeReference=Type", "variableDeclaration=connectorId type=String", "variableDeclaration=type type=Type"]} }}, {$group: { _id: "$file", hits: { $push: { line: "$line", feature: "$feature"}}}}])
     */
    val command = {
      "aggregate": FEATURE_COLLECTION_NAME, // name of the collection on which we run this command
      "pipeline": [
        { $match: {
            feature: {
              $in: features
            }
          }
        },
        { $group: {
            _id: "$file",
            hits: {
              $push :
                {
                  $line: "$line",
                  $feature: "$feature"
                }
            }
          }
        }
      ]
    }

    val futureResult: Future[List[BSONDocument]] = db.runCommand(command)

    futureResult.map {
      list => list.map {
        doc =>
          val repoAndFile = doc.getAs[String]("_id").get
          val firstSlash = repoAndFile.indexOf("/")
          val secondSlash = repoAndFile.indexOf("/", firstSlash + 1)
          val owner = repoAndFile.substring(0, firstSlash)
          val repo = repoAndFile.substring(firstSlash + 1, secondSlash)
          val file = repoAndFile.substring(secondSlash + 1)

          val hits = doc.getAs[List[(Long, String)]]("hits").get


          (Location(owner, repo, file), hits)
      }
    }
  }
}
