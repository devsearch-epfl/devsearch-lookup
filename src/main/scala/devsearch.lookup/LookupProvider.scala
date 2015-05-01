package devsearch.lookup

import akka.actor._

/**
 * This actor is the main entry point for search requests. On every request it
 * will create a new LookupMerger to execute and merge the query.
 */
class LookupProvider extends Actor with ActorLogging {
  log.info("Starting LookupProvider")

  val nPartitions = 1
  log.info(s"Creating $nPartitions PartitionLookup actor(s)")
  val partitionActors = (0 until nPartitions).map { partition =>
    context.actorOf(Props[PartitionLookup], name = s"partitionLookup$partition")
  }

  override def receive = {
    case req: SearchRequest =>
      log.info("LookupProvider: receive SearchRequest")
      context.actorOf(Props(classOf[LookupMerger], sender, partitionActors, req))
    case x => log.error(s"Received unexpected message $x")
  }
}
