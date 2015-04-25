package devsearch

import org.apache.spark.rdd.RDD

object NBestFinder {
  def getNBestMatches[K](n: Int, rdd: RDD[(K, Double)]): Array[(K, Double)] = {
    rdd.top(n)(new Ordering[(K, Double)] {
      override def compare(x: (K, Double), y: (K, Double)): Int = {
        Ordering[Double].compare(x._2, y._2)
      }
    })
  }
}
