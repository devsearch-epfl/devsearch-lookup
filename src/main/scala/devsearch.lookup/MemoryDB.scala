package devsearch.lookup

import devsearch.parsers.Languages

import scala.concurrent.Future

object MemoryDB {
  val memdb: Map[String, List[(Location, Int)]] = ???

  def getMatchesFromDb(features: Set[String], langFilter: Set[String]): Future[Stream[DocumentHits]] = {
    val langs = langFilter.map(Languages.extension).flatten

    val result = features.flatMap(feat => memdb.getOrElse(feat, List()).flatMap {
      case (loc,line) if Languages.guess(loc.file).exists(langFilter) => Some(loc, Hit(line,feat))
      case _ => None
    }).groupBy(_._1).map { case (loc, tuples) => DocumentHits(loc, tuples.map(_._2).toStream) }.toStream

    Future.successful(result)
  }

}
