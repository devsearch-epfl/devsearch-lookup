package devsearch.lookup
import devsearch.features.Feature

case class SearchRequest(features: Set[Feature], lang: Set[String], start: Int, len : Int)

sealed trait SearchResult
case class SearchResultSuccess(entries: Seq[SearchResultEntry], count: Long) extends SearchResult
case class SearchResultError(error: String) extends SearchResult

case class SearchResultEntry(user: String, repo: String, path: String, lineStart: Int, lineEnd: Int, score: Float, scoreBreakDown: Map[String, Double], featureList: Set[Feature])
