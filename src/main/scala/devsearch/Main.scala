package devsearch

import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.contrib.pattern.ClusterReceptionistExtension
import akka.util.Timeout
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup(Seq("2555"))
    else
      startup(args)
  }

  def startup(ports: Seq[String]): Unit = {
    ports foreach { port =>
      // Override the configuration of the port
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load())

      // Create an Akka system
      val system = ActorSystem("lookupCluster", config)
      val lookup = system.actorOf(Props[DummySearchActor], name = "lookup")
      ClusterReceptionistExtension(system).registerService(lookup)

    }
  }
}
