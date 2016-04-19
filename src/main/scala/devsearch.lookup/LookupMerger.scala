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
  context.setReceiveTimeout(14.seconds)

  var results: Seq[SearchResultEntry] = Seq()
  var nResponses : Int = 0
  var totalCount: Long = 0
  override def receive = {
    case SearchResultSuccess(partitionResult, count) =>
      log.info("LookupMerger: receive SearchResultSuccess (millis=" + System.currentTimeMillis + ")")
      results ++= partitionResult
      nResponses += 1
      totalCount += count
      if(nResponses == nPartitions){
        log.info("LookUpMerger: All partitions provided, sending the data back (millis=" + System.currentTimeMillis + ")")
        mergeAndReply
      }
    case SearchResultError(message) =>
      requestor ! SearchResultError(s"One partition returned an error: $message")
      context.stop(self)
    case ReceiveTimeout =>
      log.info("LookUpMerger: Timeout expires")
      if(results.nonEmpty){
         mergeAndReply
      }else {
        requestor ! SearchResultError(s"Timeout: $nResponses out of $nPartitions partitions replied on time.")
        context.stop(self)
      }
    case x => log.error(s"Received unexpected message $x")

  }

  def mergeAndReply: Unit = {
    val (res: Seq[SearchResultEntry], _) = FindNBest(results.toStream, {x: SearchResultEntry => x.score}, 10)
    val sortedResult = res.sortBy( - _.score)

    requestor ! SearchResultSuccess( sortedResult, totalCount )
    context.stop(self)
  }

}
