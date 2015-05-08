package devsearch.lookup

import reactivemongo.bson._

import devsearch.parsers.Languages

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Hit(line: Int, feature: String)
case class DocumentHits(location: Location, hits: Stream[Hit])

/**
 * Interract with the db to fetch files and line for a query
 */
object FeatureDB {

  val FEATURE_COLLECTION_NAME = "features"

  /**
   * fetches matches from the DB
   * @param features a list of feature index
   * @return A stream of ("owner/repo/path/to/file", List((featureIndex, lineNb)))
   */
  def getMatchesFromDb(features: Set[String], langFilter: Seq[String]): Future[Stream[DocumentHits]] = {

    val langs = langFilter.map(Languages.extension).flatten
    val query = if (!langs.isEmpty) {
      BSONDocument(
        "feature" -> BSONDocument(
          "$in" -> features),
        "file" -> BSONDocument(
          "$regex" -> BSONRegex(".(?:" + langs.mkString("|") + ")$","g")
        ))
    } else {
      BSONDocument(
        "feature" -> BSONDocument(
          "$in" -> features))
    }

//    println(BSONDocument.pretty(query))



    /*
      Performs an aggregation on the db to fetch each matched files with a list of lineNb and featurename
      Example in the mongo shell:
      db.features.aggregate([ {$match: { feature: { $in: ["dummyfeature1", "dummyfeature2", "dummyfeature3", "dummyfeature4"]} }}, {$group: { _id: "$file", hits: { $push: { line: "$line", feature: "$feature"}}}}])
      more info : http://reactivemongo.org/releases/0.10/documentation/advanced-topics/commands.html
     */

    val command = BSONDocument(
      "aggregate" -> FEATURE_COLLECTION_NAME, // name of the collection on which we run this command
      "pipeline" -> BSONArray(
        BSONDocument(
          "$match" -> query),
        BSONDocument(
          "$group" -> BSONDocument(
            "_id" -> "$file",
            "hits" -> BSONDocument(
              "$push" -> BSONDocument(
                "line" -> "$line",
                "feature" -> "$feature"))))
      )
    )

    val futureResult: Future[BSONDocument] = RawDB.run(command)

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
  }
}
