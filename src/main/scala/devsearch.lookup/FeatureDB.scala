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
  val LOCAL_OCCURENCES_COLLECTION_NAME = "local_occ" //TODO same as in someone's? script
  val GLOBAL_OCCURENCES_COLLECTION_NAME = "global_occ" //TODO

  /**
   * fetches number of occurrences from DB
   * @param collection name of collection to lookup
   * @param features set of features to get counts for
   * @param languages set of languages to get counts for (empty means all languages)
   * @return A map from (feature, language) pair to number of occurrences
   */
  def getFeatureOccurrenceCount(collection: String, features: Set[String], languages: Set[String]): Future[Map[(String, String), Long]] = {
    val command = BSONDocument(
      "aggregate" -> collection,
      "pipeline" -> BSONArray(
        BSONDocument(
          "$match" -> (
            BSONDocument(
              "feature" -> BSONDocument("$in" -> features)
            ) ++ (
              if (!languages.isEmpty) BSONDocument(
                "language" -> BSONDocument("$in" -> languages)
              ) else BSONDocument()
            )
          )
        )
      )
    )

    val futureResult = RawDB.run(command)

    // convert BSON result to a simple map
    futureResult.map(result => {
      result.getAs[BSONArray]("result").get.values.map{
        case entry: BSONDocument =>
          (
            entry.getAs[String]("feature").getOrElse(throw new Exception("malformed data: feature key not present")),
            entry.getAs[String]("language").getOrElse(throw new Exception("malformed data: language key not present"))
          ) -> entry.getAs[Int]("count").getOrElse(throw new Exception("malformed data: count")).toLong
      }.toMap
    })
  }

  /**
   * fetches matches from the DB
   * @param rareFeatures a set of rare features to select by (union), nonempty
   * @param commonFeatures a set of common features to filter by, may be empty
   * @param langFilter a set of languages which should be included (empty means all languages)
   * @return A stream of ("owner/repo/path/to/file", List((featureIndex, lineNb)))
   */
  def getMatchesFromDb(rareFeatures: Set[String], commonFeatures: Set[String], langFilter: Set[String]): Future[Stream[DocumentHits]] = {
    val features = rareFeatures // FIXME

    val langs = langFilter.map(Languages.extension).flatten
    val query = BSONDocument(
      "feature" -> (
        BSONDocument(
          "$in" -> rareFeatures
        ) ++ (
          if (!langs.isEmpty) BSONDocument(
            "file" -> BSONDocument(
              "$regex" -> BSONRegex(".(?:" + langs.mkString("|") + ")$","g")
            )
          ) else BSONDocument()
        )
      )
    )

//    println(BSONDocument.pretty(query))

    /*
      Performs an aggregation on the db to fetch each matched files with a list of lineNb and featurename
      Example in the mongo shell:
      db.features.aggregate([ {$match: { feature: { $in: ["dummyfeature1", "dummyfeature2", "dummyfeature3", "dummyfeature4"]} }}, {$group: { _id: "$file", hits: { $push: { line: "$line", feature: "$feature"}}}}])
      more info : http://reactivemongo.org/releases/0.10/documentation/advanced-topics/commands.html
     */

//    val command = BSONDocument(
//      "aggregate" -> FEATURE_COLLECTION_NAME, // name of the collection on which we run this command
//      "pipeline" -> BSONArray(
//        BSONDocument(
//          "$match" -> query),
//        BSONDocument(
//          "$group" -> BSONDocument(
//            "_id" -> "$file",
//            "hits" -> BSONDocument(
//              "$push" -> BSONDocument(
//                "line" -> "$line",
//                "feature" -> "$feature"))))
//      )
//    )

    val rareMatchesCommand = BSONDocument(
      "distinct" -> FEATURE_COLLECTION_NAME, // name of the collection on which we run this command
      "key" -> "$file",
      "query" -> BSONDocument(
          "$match" -> query)
      )

    val futureRareMatchResult = RawDB.run(rareMatchesCommand)

    for  {
      res <- futureRareMatchResult
      answers <- {
        val rareMatchResult: Stream[String] = res.getAs[BSONArray]("result").get.values.map{
          // no need for asInstanceOf ?
          case entry: BSONString => entry.value
        }


        val fetchAllFeatures = BSONDocument(
          "aggregate" -> FEATURE_COLLECTION_NAME, // name of the collection on which we run this command
          "pipeline" -> BSONArray(
            BSONDocument(
              "$match" -> (
                BSONDocument(
                  "feature" -> BSONDocument( "$in" -> (rareFeatures ++ commonFeatures) ),
                  "file" -> BSONDocument( "$in" -> rareMatchResult )
                ) ++ (
                  if (langs.nonEmpty) BSONDocument(
                    "file" -> BSONDocument(
                      "$regex" -> BSONRegex(".(?:" + langs.mkString("|") + ")$","g")
                    )
                  ) else BSONDocument()
                  )
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

        val futureResult: Future[BSONDocument] = RawDB.run(fetchAllFeatures)

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
    } yield answers

//    convert BSON result to a simple map
//    futureRareMatchResult.map(
//      res => {
//        val rareMatchResult: Stream[String] = res.getAs[BSONArray]("result").get.values.map{
//          // no need for asInstanceOf ?
//          case entry: BSONString => entry.value
//        }
//
//
//        val fetchAllFeatures = BSONDocument(
//          "aggregate" -> FEATURE_COLLECTION_NAME, // name of the collection on which we run this command
//          "pipeline" -> BSONArray(
//            BSONDocument(
//              "$match" -> (
//                BSONDocument(
//                  "feature" -> BSONDocument( "$in" -> (rareFeatures ++ commonFeatures) ),
//                  "file" -> BSONDocument( "$in" -> rareMatchResult )
//                 ) ++ (
//                  if (langs.nonEmpty) BSONDocument(
//                    "file" -> BSONDocument(
//                      "$regex" -> BSONRegex(".(?:" + langs.mkString("|") + ")$","g")
//                    )
//                  ) else BSONDocument()
//                )
//              )
//            ),
//            BSONDocument(
//              "$group" -> BSONDocument(
//                "_id" -> "$file",
//                "hits" -> BSONDocument(
//                  "$push" -> BSONDocument(
//                    "line" -> "$line",
//                    "feature" -> "$feature"))))
//          )
//        )
//
//        val futureResult: Future[BSONDocument] = RawDB.run(fetchAllFeatures)
//
//        implicit object HitReader extends BSONDocumentReader[Hit] {
//          def read(doc: BSONDocument): Hit = {
//            Hit(
//              doc.getAs[Int]("line").get,
//              doc.getAs[String]("feature").get
//            )
//          }
//        }
//
//        implicit object DocumentHitsReader extends BSONDocumentReader[DocumentHits] {
//          def read(doc: BSONDocument): DocumentHits = {
//
//            val repoAndFile = doc.getAs[String]("_id").get
//            val firstSlash = repoAndFile.indexOf("/")
//            val secondSlash = repoAndFile.indexOf("/", firstSlash + 1)
//
//            val hitStream: Stream[Hit] =  doc.getAs[BSONArray]("hits").map {
//              docArray => docArray.values.map{
//                docOption => docOption.seeAsOpt[BSONDocument].map(BSON.readDocument[Hit])
//              }.flatten
//            }.getOrElse(Stream())
//
//
//            DocumentHits(
//              Location(
//                repoAndFile.substring(0, firstSlash),
//                repoAndFile.substring(firstSlash + 1, secondSlash),
//                repoAndFile.substring(secondSlash + 1)
//              ),
//              hitStream
//            )
//          }
//        }
//
//        futureResult.map {
//          list => list.getAs[BSONArray]("result").map {
//            docArray => docArray.values.map {
//              docOption => docOption.seeAsOpt[BSONDocument].map (
//                BSON.readDocument[DocumentHits]
//              )
//            }.flatten
//          }.getOrElse(Stream())
//        }
//      }
//    )
  }
}
