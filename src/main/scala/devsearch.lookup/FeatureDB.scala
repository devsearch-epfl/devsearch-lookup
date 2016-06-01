package devsearch.lookup

import reactivemongo.bson._

import devsearch.parsers.Languages

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

import reactivemongo.core.commands.RawCommand

case class Hit(line: Int, feature: String)
case class DocumentHits(location: Location, repoRank: Double, hits: Stream[Hit])

case class FileMatch(file: String, score: Float, center: Float, std: Float, hits: Stream[Hit])

/**
 * Interract with the db to fetch files and line for a query
 */
object FeatureDB {

  val FEATURE_COLLECTION_NAME = "features"
  val LOCAL_OCCURENCES_COLLECTION_NAME = "local_occ"
  val GLOBAL_OCCURENCES_COLLECTION_NAME = "global_occ"
  val STAGE_2_LIMIT = 10000

  /**
   * fetches number of occurrences from DB
    *
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
    *
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
          feature.getAs[String]("file").getOrElse(throw new Exception("malformed data: file key not present: " + feature.toString))
        }).distinct

        rareMatchFiles.sorted.foreach(println(_))

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
                "repoRank" -> BSONDocument(
                  "$first" -> "$repoRank"),
                "hits" -> BSONDocument(
                  "$push" -> BSONDocument(
                    "line" -> "$line",
                    "feature" -> "$feature"))))
          )
        )

        val fetchAllFeatures_explain = BSONDocument(
          "$explain" -> BSONDocument(
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
                  "repoRank" -> BSONDocument(
                    "$first" -> "$repoRank"),
                  "hits" -> BSONDocument(
                    "$push" -> BSONDocument(
                      "line" -> "$line",
                      "feature" -> "$feature"))))
            )
          )
        )


        TimedFuture(RawDB.db.command(RawCommand(fetchAllFeatures_explain)), name = "explain_query").map{ case answer =>
        println("query plan: "  + BSONDocument.pretty(answer))}.recover{case e => println(e)}

        val futureResult: Future[BSONDocument] = TimedFuture(RawDB.db.command(RawCommand(fetchAllFeatures)), name = "RawQuery mongo")

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

            val repoRank = doc.getAs[Double]("repoRank").get

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
              repoRank,
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

  /**
    * Fetches matches from the database
    *
    * @param features
    * @param langFilter
    * @param skip
    * @param take
    * @return
    */
  def getBestFileMatchesFromDb(features: Set[String], langFilter: Set[String], skip: Int, take: Int): Future[Stream[FileMatch]] = {

    val langs = langFilter.map(Languages.extension).flatten

    val query = BSONDocument(
      "feature" -> BSONDocument(
        "$in" -> features
      )
    ) ++ (
      if (langs.nonEmpty) BSONDocument(
        "file" -> BSONDocument(
          "$regex" -> BSONRegex(".(?:" + langs.mkString("|") + ")$","g")
        )
      ) else BSONDocument()
      )

    val mapFunction = "function() {emit(this.file, {line: this.line, feature: this.feature, rank: this.repoRank});}";

    val redFunction =
      """function(key, values) {
        |
        |    var ret = {rank:12, list: []};
        |
        |    values.forEach(function(val) {
        |        ret.rank = val.rank;
        |
        |        if (val.list) {
        |            ret.list = ret.list.concat(val.list);
        |        } else {
        |            ret.list.push({line: val.line, feature: val.feature})
        |        }
        |    })
        |
        |    return ret;
        |}""".stripMargin;

    val finFunction =
      """function(key, reduced) {
        |
        |    list = reduced.list || [{line: reduced.line, feature: reduced.feature}];
        |    len = list.length;
        |    poses = list.map(function(e){return e.line});
        |
        |    center = Array.avg(poses);
        |    std = Array.stdDev(poses);
        |
        |
        |    return {
        |        file: key,
        |        score: reduced.rank * len,
        |        center: center,
        |        std: std,
        |        list: list
        |    }
        |}""".stripMargin;

    val TEMPORARY_QUERY = "temp_query";

    val fetchBestJob = BSONDocument(
      "find" -> TEMPORARY_QUERY,
      "sort" -> BSONDocument("value.score" -> -1),
      "limit" -> take,
      "skip" -> skip
    )

    val mapReduceJob = BSONDocument(
      "mapReduce" -> FEATURE_COLLECTION_NAME, // name of the collection on which we run this command
      "map" -> BSONString(mapFunction),
      "reduce" -> BSONString(redFunction),
      "finalize" -> BSONString(finFunction),
      "out" -> TEMPORARY_QUERY,
      "query" -> query,
      "jsMode" -> false,
      "verbose" -> true,
      "bypassDocumentValidation" -> false
    )

    for {
      mapReduceResult <- TimedFuture(RawDB.db.command(RawCommand(mapReduceJob)), name = "mapReduce mongo")
      answers <- TimedFuture({

        println("MapRed RESULT: \n" + BSONDocument.pretty(mapReduceResult))

        val topResults: Future[BSONDocument] = TimedFuture(RawDB.db.command(RawCommand(fetchBestJob)), name = "fetch best mongo")

        implicit object HitReader extends BSONDocumentReader[Hit] {
          def read(doc: BSONDocument): Hit = {
            Hit(
              doc.getAs[BSONNumberLike]("line").get.toInt,
              doc.getAs[String]("feature").get
            )
          }
        }

        implicit object FileMatchReader extends BSONDocumentReader[FileMatch] {
          def read(doc: BSONDocument): FileMatch = {

            doc.getAs[BSONDocument]("value").map{

              value =>
                val hitStream: Stream[Hit] =  value.getAs[BSONArray]("list").map {
                  docArray => docArray.values.map{
                    docOption => docOption.seeAsOpt[BSONDocument].map(BSON.readDocument[Hit])
                  }.flatten
                }.getOrElse(Stream())

                FileMatch(
                  value.getAs[String]("file").get,
                  value.getAs[BSONNumberLike]("score").get.toFloat,
                  value.getAs[BSONNumberLike]("center").get.toFloat,
                  value.getAs[BSONNumberLike]("std").get.toFloat,
                  hitStream
                )
            }
          }.get
        }

        topResults.map {
          queryResult =>
            queryResult.getAs[BSONDocument]("cursor").map {
              cursor =>
                cursor.getAs[BSONArray]("firstBatch").map{
                  docArray =>
                    println("Result Size: " + docArray.values.length)
                    docArray.values.flatMap {
                      docOption =>
                        println("Found Raw DOC")
                        docOption.seeAsOpt[BSONDocument].map {
                          doc =>
                            println("Found DOC")
                            BSON.readDocument[FileMatch](doc)
                        }
                    }

                }.getOrElse(Stream())

            }.getOrElse(Stream())
        }

      }, name = "final pipeline (fetch best + parse)")
    } yield answers
  }
}
