package devsearch.lookup

import akka.actor._
import scala.concurrent.duration._
/**
 * This actor is responsible for contacting each partition, to merge their
 * results and to send it back to the requestor (Play app).
 */
class LookupMerger(
  val requestor: ActorRef
) extends Actor with ActorLogging {

  log.info("Starting LookupMerger")
  context.setReceiveTimeout(3.seconds)

  var results: Seq[SearchResultEntry] = Seq()

  override def receive = {
    case SearchResultSuccess(partitionResult) =>
      log.info("LookupMerger: receive SearchResultSuccess")
      results ++= partitionResult
    case SearchResultError(message) =>
      requestor ! SearchResultError(s"one partition returned an error: $message")
      context.stop(self)
    case ReceiveTimeout =>
      log.info("LookUpMerger: ends of the lookup, sending the data back")
      requestor ! SearchResultSuccess(FindNBest(results, 10).sortBy( - _.score))
      context.stop(self)
    case x => log.error(s"Received unexpected message $x")

  }
}
