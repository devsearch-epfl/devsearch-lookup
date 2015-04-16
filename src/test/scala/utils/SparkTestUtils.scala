package utils

import org.apache.spark._
import org.apache.log4j._

object SparkTestUtils {
  def withSpark(f: SparkContext => Unit) {

    // TODO: logger silencing is not working
    val loggersToSilent = Seq("spark", "org.eclipse.jetty", "akka")
    val loggersAndLevels = loggersToSilent.map(loggerName => {
      val logger = Logger.getLogger(loggerName)
      val prevLevel = logger.getLevel()
      (logger, prevLevel)
    })
    loggersAndLevels.foreach{case (logger, _) =>
      logger.setLevel(Level.WARN)
    }

    val sc = new SparkContext("local[4]", "unit-test")
    try {
      f(sc)
    } finally {
      sc.stop
      loggersAndLevels.foreach{case (logger, level) =>
        logger.setLevel(level)
      }
    }
  }
}
