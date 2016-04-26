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

  def randomUuid() = java.util.UUID.randomUUID().toString

  override def receive = {

    case req: SearchRequest =>
      log.info("LookupProvider: receive SearchRequest (millis=" + System.currentTimeMillis + ")")
      if (req.features.isEmpty)

        // handles the empty query case
        sender ! SearchResultSuccess(Seq(), 0)
      else {

        // Creates a lookupMerger actor that will query and merge results from all PartitionManager Actors on the cluster
        val merger = context.actorOf(Props(classOf[LookupMerger], sender, maxPartitions), name = "merger-" + randomUuid())
        partitionManagers.tell(req, merger)
      }

    case x => log.error(s"Received unexpected message $x")
  }
}
