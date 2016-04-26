package devsearch.lookup

import scala.concurrent._
import akka.event.Logging

object TimedFuture {
  def apply[T](future: Future[T], name: String)(implicit ec: ExecutionContext): Future[T] = {
    val start = System.nanoTime
    future.onComplete {
      case _ =>
        val end = System.nanoTime
        val elapsedSeconds = (end - start) * 0.000000001
        println("====> " + name + " finished in " + elapsedSeconds + " seconds")
    }
    future
  }
}
