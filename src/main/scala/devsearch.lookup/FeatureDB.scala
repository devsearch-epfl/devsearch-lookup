package devsearch.lookup

import reactivemongo.api.MongoDriver
import reactivemongo.bson.{BSONArray, BSONDocument}
import reactivemongo.core.commands.RawCommand

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

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
  def getMatchesFromDb(features: Seq[String]): Stream[(Location, Stream[(Long, String)])] = {

    /*
      Performs an aggregation on the db to fetch each matched files with a list of lineNb and featurename
      Example in the mongo shell:
      db.features.aggregate([ {$match: { feature: { $in: ["className=ExampleModule", "typeReference=Type", "variableDeclaration=connectorId type=String", "variableDeclaration=type type=Type"]} }}, {$group: { _id: "$file", hits: { $push: { line: "$line", feature: "$feature"}}}}])
      more info : http://reactivemongo.org/releases/0.10/documentation/advanced-topics/commands.html
     */
    val command = BSONDocument(
      "aggregate" -> FEATURE_COLLECTION_NAME, // name of the collection on which we run this command
      "pipeline" -> BSONArray(
        BSONDocument(
          "$match" -> BSONDocument(
            "feature" -> BSONDocument(
              "$in" -> "features"))),
        BSONDocument(
          "$group" -> BSONDocument(
            "_id" -> "$file",
            "hits" -> BSONDocument(
              "$push" -> BSONDocument(
                "$line" -> "$line",
                "$feature" -> "$feature"))))
      )
    )

    val futureResult: Future[BSONDocument] = db.command(RawCommand(command))

    futureResult.map {
      list => list.getAs[BSONArray]("result").map {
        docArray => docArray.values.map {
          doc =>
            //TODO use seeAsOpt and readers
            val repoAndFile = doc.getAS[String]("_id").get
            val firstSlash = repoAndFile.indexOf("/")
            val secondSlash = repoAndFile.indexOf("/", firstSlash + 1)
            val owner = repoAndFile.substring(0, firstSlash)
            val repo = repoAndFile.substring(firstSlash + 1, secondSlash)
            val file = repoAndFile.substring(secondSlash + 1)

            val hits = doc.getAs[BSONArray]("hits").map {
              entry => (entry.getAs[Long]("line"), entry.getAs[String]("feature"))
            }


            (Location(owner, repo, file), hits)
        }
      }
    }
  }
}
