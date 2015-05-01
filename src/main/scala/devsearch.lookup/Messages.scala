package devsearch.lookup

case class SearchRequest(features: Seq[String])

sealed trait SearchResult
case class SearchResultSuccess(entries: Seq[SearchResultEntry]) extends SearchResult
case class SearchResultError(error: String) extends SearchResult

case class SearchResultEntry(repo: String, path: String, line: Int, score: Float)
