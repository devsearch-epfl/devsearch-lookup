package devsearch.lookup

import akka.actor._
import com.typesafe.config._
import akka.contrib.pattern.ClusterReceptionistExtension
import akka.util.Timeout
import scala.concurrent.duration.Duration
import java.net._
import akka.cluster.Cluster;
import scopt.OptionParser

object Main {

  def main(args: Array[String]): Unit = {

    val parser: OptionParser[Config] = new OptionParser[Config]("LookupCluster") {
      opt[Unit]('c', "cluster").text("run the cluster on multiple machines").action((_, _) =>
        ConfigFactory.parseString(
          s"akka.remote.netty.tcp.hostname=${InetAddress.getLocalHost.getHostName}"
        ) withFallback ConfigFactory.load("deployment"))
      opt[Unit]('s', "slave").text("slave node").action((_, c) =>
        ConfigFactory.parseString("devsearch.slave=true") withFallback c)
      opt[Int]('p', "port").text("port of the netty server").action((port, c) =>
        ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port") withFallback c)
      opt[Int]('n', "nbpart").text("number of partitions").action((part, c) =>
        ConfigFactory.parseString(s"devsearch.nbPartitions=$part") withFallback c)
    }

    def fail(msg: String): Nothing = {
      Console.err.println(s"ERROR : $msg")
      sys.exit(1)
    }

    val conf = parser.parse(args, ConfigFactory.load("application")) getOrElse sys.exit(1)
    println(conf.root().render())
    val system = ActorSystem("lookupCluster", conf)
    if(conf.getBoolean("devsearch.slave")){
      system.actorOf(Props[PartitionManager], name = "partitionManager")
    } else {
      val lookup = system.actorOf(Props(classOf[LookupProvider], conf.getInt("devsearch.nbPartitions")), name = "lookup")
      ClusterReceptionistExtension(system).registerService(lookup)
    }
    system.awaitTermination()

  }



}
