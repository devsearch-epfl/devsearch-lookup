package devsearch.lookup

case class SearchRequest(features: Set[String], lang: Seq[String])

sealed trait SearchResult
case class SearchResultSuccess(entries: Seq[SearchResultEntry]) extends SearchResult
case class SearchResultError(error: String) extends SearchResult

case class SearchResultEntry(user: String, repo: String, path: String, line: Int, score: Float)
