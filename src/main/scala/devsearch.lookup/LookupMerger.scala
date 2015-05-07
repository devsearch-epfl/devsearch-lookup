package devsearch.lookup

import akka.actor._
import scala.concurrent.duration._
/**
 * This actor is responsible for contacting each partition, to merge their
 * results and to send it back to the requestor (Play app).
 */
class LookupMerger(
  val requestor: ActorRef, val nPartitions : Int
) extends Actor with ActorLogging {

  log.info("Starting LookupMerger")
  context.setReceiveTimeout(10.millis)

  var results: Seq[SearchResultEntry] = Seq()
  var nResponses : Int = 0
  override def receive = {
    case SearchResultSuccess(partitionResult) =>
      log.info("LookupMerger: receive SearchResultSuccess")
      results ++= partitionResult
      nResponses += 1
      if(nResponses == nPartitions){
        log.info("LookUpMerger: All partitions provided, sending the data back")
        mergeAndReply
      }
    case SearchResultError(message) =>
      requestor ! SearchResultError(s"one partition returned an error: $message")
      context.stop(self)
    case ReceiveTimeout =>
      log.info("LookUpMerger: Timeout expires")
      requestor ! SearchResultError(s"Timeout: $nResponses out of $nPartitions partitions replied on time.")
      context.stop(self)
    case x => log.error(s"Received unexpected message $x")

  }

  def mergeAndReply: Unit = {
    requestor ! SearchResultSuccess(
      FindNBest(results.toStream, {x: SearchResultEntry => x.score}, 10).sortBy( - _.score)
    )
    context.stop(self)
  }

}
