package devsearch.lookup

import akka.actor._

class LookupMerger(
  requestor: ActorRef, partitionActors: Seq[ActorRef], request: SearchRequest
) extends Actor with ActorLogging {

  log.info("Starting LookupMerger")

  partitionActors.foreach(_ ! request)
  var results: Seq[SearchResult] = Seq()

  override def receive = {
    case partitionResult: SearchResultSuccess =>
      log.info("LookupMerger: receive SearchResultSuccess")

      results = results :+ partitionResult
      if (results.length == partitionActors.length) {
        requestor ! results(0) // TODO: merge results
        context.stop(self)
      }
    case SearchResultError(message) =>
      requestor ! SearchResultError(s"one partition returned an error: $message")
      context.stop(self)
    case x => log.error(s"Received unexpected message $x")
  }
}
