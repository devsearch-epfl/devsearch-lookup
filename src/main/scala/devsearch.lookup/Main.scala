package devsearch.lookup

import akka.actor._
import com.typesafe.config._
import akka.contrib.pattern.ClusterReceptionistExtension
import akka.util.Timeout
import scala.concurrent.duration.Duration
import java.net._
import akka.cluster.Cluster;


object Main {
  def main(args: Array[String]): Unit = {
    if(args.isEmpty) runMaster(false)
    else args(0) match {
      case "--slave" => runSlave(
          ConfigFactory.parseString(
            s"akka.remote.netty.tcp.hostname=${InetAddress.getLocalHost.getHostAddress}"
          ).withFallback(ConfigFactory.load("deployment")))
      case "--master" => runMaster(true)
      case _ => runSlave(ConfigFactory.parseString(
                  s"akka.remote.netty.tcp.port=${args(0).toInt}"
                ).withFallback(ConfigFactory.load("application")))
    }
  }

  def runSlave(config: Config): Unit = {

    val system = ActorSystem("lookupCluster", config)
    val partitionManager = system.actorOf(Props[PartitionManager], name = "partitionManager")
    system.awaitTermination()
  }

  def runMaster(deployment : Boolean) : Unit = {
    val ip = InetAddress.getLocalHost.getHostAddress
    val config = if(deployment) ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + ip).
      withFallback(ConfigFactory.load("deployment"))
      else ConfigFactory.load("application")
    val system = ActorSystem("lookupCluster", config)
    val lookup = system.actorOf(Props[LookupProvider], name = "lookup")
    ClusterReceptionistExtension(system).registerService(lookup)
    system.awaitTermination()
  }


}
