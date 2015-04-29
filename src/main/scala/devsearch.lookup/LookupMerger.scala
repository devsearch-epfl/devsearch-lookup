package devsearch.lookup

import akka.actor._

/**
 * This actor is responsible for contacting each partition, to merge their
 * results and to send it back to the requestor (Play app).
 */
class LookupMerger(
  requestor: ActorRef, partitionActors: Seq[ActorRef], request: SearchRequest
) extends Actor with ActorLogging {

  log.info("Starting LookupMerger")

  partitionActors.foreach(_ ! request)
  var numReceived: Int = 0
  var results: Seq[SearchResultEntry] = Seq()

  override def receive = {
    case SearchResultSuccess(partitionResult) =>
      log.info("LookupMerger: receive SearchResultSuccess")

      numReceived += 1
      results ++= partitionResult
      if (numReceived == partitionActors.length) {
        requestor ! SearchResultSuccess(FindNBest(results, 10).sortBy(-_.score))
        context.stop(self)
      }
    case SearchResultError(message) =>
      requestor ! SearchResultError(s"one partition returned an error: $message")
      context.stop(self)
    case x => log.error(s"Received unexpected message $x")
  }
}
