package devsearch.lookup

import reactivemongo.bson._

import devsearch.parsers.Languages

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

import reactivemongo.core.commands.RawCommand

case class Hit(line: Int, feature: String)
case class DocumentHits(location: Location, hits: Stream[Hit])

/**
 * Interract with the db to fetch files and line for a query
 */
object FeatureDB {

  val FEATURE_COLLECTION_NAME = "features"
  val LOCAL_OCCURENCES_COLLECTION_NAME = "local_occ"
  val GLOBAL_OCCURENCES_COLLECTION_NAME = "global_occ"
  val STAGE_2_LIMIT = 1000

  /**
   * fetches number of occurrences from DB
   * @param collection name of collection to lookup
   * @param features set of features to get counts for
   * @param languages set of languages to get counts for (empty means all languages)
   * @return A map from (feature, language) pair to number of occurrences
   */
  def getFeatureOccurrenceCount(collection: String, features: Set[String], languages: Set[String]): Future[Map[(String, String), Long]] = {
    val langs = languages.map(Languages.extension).flatten

    val query = BSONDocument(
        "feature" -> BSONDocument("$in" -> features)
      ) ++ (
        if (langs.nonEmpty) BSONDocument(
          "language" -> BSONDocument("$in" -> languages)
        ) else BSONDocument()
      )

    val futureResult = RawDB.db(collection).find(query).cursor[BSONDocument].collect[List]()

    futureResult.map(
      _.map(feature =>
        (
          feature.getAs[String]("feature").getOrElse(throw new Exception("malformed data: feature key not present")),
          feature.getAs[String]("language").getOrElse(throw new Exception("malformed data: language key not present"))
        ) -> feature.getAs[Int]("count").getOrElse(throw new Exception("malformed data: count")).toLong
      ).toMap
    )
  }

  /**
   * fetches matches from the DB
   * @param rareFeatures a set of rare features to select by (union), nonempty
   * @param commonFeatures a set of common features to filter by, may be empty
   * @param langFilter a set of languages which should be included (empty means all languages)
   * @return A stream of ("owner/repo/path/to/file", List((featureIndex, lineNb)))
   */
  def getMatchesFromDb(rareFeatures: Set[String], commonFeatures: Set[String], langFilter: Set[String]): Future[Stream[DocumentHits]] = {
    val langs = langFilter.map(Languages.extension).flatten
    val query = BSONDocument(
        "feature" -> BSONDocument(
          "$in" -> (if (rareFeatures.nonEmpty) rareFeatures else commonFeatures)
        )
      ) ++ (
        if (langs.nonEmpty) BSONDocument(
          "file" -> BSONDocument(
            "$regex" -> BSONRegex(".(?:" + langs.mkString("|") + ")$","g")
          )
        ) else BSONDocument()
      )

    for {
      limitedFiles <- TimedFuture(RawDB.db(FEATURE_COLLECTION_NAME).find(query).cursor[BSONDocument].collect[List](STAGE_2_LIMIT), name = "limited files")
      answers <- TimedFuture({
        val rareMatchFiles: List[String] = limitedFiles.map(feature => {
          feature.getAs[String]("file").getOrElse(throw new Exception("malformed data: file key not present"))
        })

        println("got all rare matches")

        val fetchAllFeatures = BSONDocument(
          "aggregate" -> FEATURE_COLLECTION_NAME, // name of the collection on which we run this command
          "pipeline" -> BSONArray(
            BSONDocument(
              "$match" -> BSONDocument(
                "file" -> BSONDocument( "$in" -> rareMatchFiles ),
                "feature" -> BSONDocument( "$in" -> (rareFeatures ++ commonFeatures) )
              )
            ),
            BSONDocument(
              "$group" -> BSONDocument(
                "_id" -> "$file",
                "hits" -> BSONDocument(
                  "$push" -> BSONDocument(
                    "line" -> "$line",
                    "feature" -> "$feature"))))
          )
        )
        println("Query: " + BSONDocument.pretty(fetchAllFeatures))

        val futureResult: Future[BSONDocument] = RawDB.db.command(RawCommand(fetchAllFeatures))

        /*Await.ready(futureResult, 60.seconds)
        println("ready!")
        ???*/

        implicit object HitReader extends BSONDocumentReader[Hit] {
          def read(doc: BSONDocument): Hit = {
            Hit(
              doc.getAs[Int]("line").get,
              doc.getAs[String]("feature").get
            )
          }
        }

        implicit object DocumentHitsReader extends BSONDocumentReader[DocumentHits] {
          def read(doc: BSONDocument): DocumentHits = {

            val repoAndFile = doc.getAs[String]("_id").get
            val firstSlash = repoAndFile.indexOf("/")
            val secondSlash = repoAndFile.indexOf("/", firstSlash + 1)

            val hitStream: Stream[Hit] =  doc.getAs[BSONArray]("hits").map {
              docArray => docArray.values.map{
                docOption => docOption.seeAsOpt[BSONDocument].map(BSON.readDocument[Hit])
              }.flatten
            }.getOrElse(Stream())


            DocumentHits(
              Location(
                repoAndFile.substring(0, firstSlash),
                repoAndFile.substring(firstSlash + 1, secondSlash),
                repoAndFile.substring(secondSlash + 1)
              ),
              hitStream
            )
          }
        }

        futureResult.map {
          list => list.getAs[BSONArray]("result").map {
            docArray => docArray.values.map {
              docOption => docOption.seeAsOpt[BSONDocument].map (
                BSON.readDocument[DocumentHits]
              )
            }.flatten
          }.getOrElse(Stream())
        }
      }, name = "final pipeline")
    } yield answers
  }
}
