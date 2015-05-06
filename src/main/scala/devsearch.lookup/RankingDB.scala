package devsearch.lookup

import reactivemongo.bson.{BSON, BSONDocument, BSONDocumentReader}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RepoRanking(owner: String, repo: String, score: Double)

/**
 * Interract with the db to fetch files and line for a query
 */
object RankingDB {

  val RANKING_COLLECTION_NAME = "rankings"

  val collection = RawDB.getCollection(RANKING_COLLECTION_NAME)

  def getRanking(location: Location): Future[Double] = {

    val query = BSONDocument(
      "ownerRepo" -> (location.owner + "/" + location.repo))

    val futureResult: Future[List[BSONDocument]] = collection.find(query).cursor[BSONDocument].collect[List](1)

    implicit object RepoRankingReader extends BSONDocumentReader[RepoRanking] {
      def read(doc: BSONDocument): RepoRanking = {

        val repo = doc.getAs[String]("ownerRepo").get
        val firstSlash = repo.indexOf("/")

        RepoRanking(
          repo.substring(0, firstSlash),
          repo.substring(firstSlash + 1),
          doc.getAs[Double]("score").get
        )
      }
    }

    futureResult.map {
      docArray: List[BSONDocument] =>
        docArray.map{
          doc => BSON.readDocument[RepoRanking](doc).score
        } match {
          case score::rest => score
          case _ => 0 // If we don't have the score for them, they will just be assigned to 0
        }
    }
  }
}
