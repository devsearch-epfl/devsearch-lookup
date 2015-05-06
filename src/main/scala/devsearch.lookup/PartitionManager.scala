package devsearch.lookup

import akka.actor._
import reactivemongo.api._
import reactivemongo.bson._
import akka.routing._
/**SmallestMailboxRoutingLogic
 * This actor manages a partition node of the cluster. It starts at the beginning
 * of a slave nodes. It creates a partition lookup on request.
 */
class PartitionManager extends Actor with ActorLogging {
  import context._
  log.info("Starting Partition Manager")


  val driver = new MongoDriver(context.system)
  val connection = driver.connection(List("localhost"))
  val db = connection("devsearch")
  val lookup = context.actorOf(Props(classOf[PartitionLookup]).withRouter(SmallestMailboxPool(10)), name = "partitionLookup")
  //In prestart add a few checks on the sanity of the databases

  override def receive = {
    case req: SearchRequest =>
      log.info("Receive SearchRequest, forwarding")
      lookup forward req
    case x => log.error(s"Received unexpected message $x")
  }
}
