package devsearch

import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.contrib.pattern.ClusterReceptionistExtension
import akka.util.Timeout
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) 2555 else args(0).toInt
    run(port)
  }

  def run(port: Int): Unit = {
    // Override the configuration of the port
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())

    // Create an Akka system
    val system = ActorSystem("lookupCluster", config)
    val lookup = system.actorOf(Props[DummySearchActor], name = "lookup")
    ClusterReceptionistExtension(system).registerService(lookup)
    system.awaitTermination()
  }
}
