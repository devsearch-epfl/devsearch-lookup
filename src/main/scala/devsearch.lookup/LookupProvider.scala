package devsearch.lookup

import akka.actor._
import akka.routing.FromConfig
import akka.routing.ConsistentHashingRouter._
import akka.routing.Broadcast
/**
 * This actor is the main entry point for search requests. On every request it
 * will create a new LookupMerger to execute and merge the query.
 */
class LookupProvider(val maxPartitions: Int) extends Actor with ActorLogging {
  log.info("Starting LookupProvider")

  val partitionManagers = context.actorOf(FromConfig.props(),
    name = "partitionRouter")

  override def receive = {
    case req: SearchRequest =>
      log.info("LookupProvider: receive SearchRequest")
      val merger = context.actorOf(Props(classOf[LookupMerger], sender, maxPartitions), name = "merger")
      partitionManagers.tell(req, merger)
    case x => log.error(s"Received unexpected message $x")
  }
}
