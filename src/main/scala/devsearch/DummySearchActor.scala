package devsearch

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.ActorLogging
import akka.actor.Actor


class DummySearchActor extends Actor with ActorLogging {

  def receive = {
    case _ => log.info("Someone needs me to do something, but I am just an idiot")
      sender ! Left("Sorry, I am a dummy!")
    
  }
}