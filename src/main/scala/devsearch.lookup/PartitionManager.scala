package devsearch.lookup

import akka.actor._
import akka.routing._
/**SmallestMailboxRoutingLogic
 * This actor manages a partition node of the cluster. It starts at the beginning
 * of a slave nodes. It creates a partition lookup on request.
 */
class PartitionManager extends Actor with ActorLogging {
  log.info("Starting Partition Manager")

  val lookup = context.actorOf(Props(classOf[PartitionLookup]).withRouter(SmallestMailboxPool(1)), name = "partitionLookup")
  //In prestart add a few checks on the sanity of the databases

  override def receive = {
    case req: SearchRequest =>
      log.info("Receive SearchRequest, forwarding")
      lookup forward req
    case x => log.error(s"Received unexpected message $x")
  }
}
