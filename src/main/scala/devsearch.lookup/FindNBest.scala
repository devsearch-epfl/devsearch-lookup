package devsearch.lookup

import java.util.Random

/**
 * QuickSelect algorithm
 * http://en.wikipedia.org/wiki/Quickselect
 */
object FindNBest {
  val rand = new Random(System.currentTimeMillis())

  def apply(list: Seq[SearchResultEntry], n: Int): Seq[SearchResultEntry] = {
    list.take(n)
    /* todo: fix this (infinite recursion)
    if (n <= 0) return Seq()
    if (list.size <= n) return list
    if (list.size <= 5) return list.sortBy(_.score).takeRight(n)
    val pivot = list(rand.nextInt(list.size)).score
    val (left, right) = list.partition(_.score >= pivot)
    apply(left, n) ++ apply(right, n - left.size)
    */
  }
}
