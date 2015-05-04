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

object FindNBestNew {
  type Entry = (String, List[(String, List[Int])], Float)

  //TODO which version is fastest?

  def dropSmallestV1(list: List[Entry], cmp: Entry => Float): List[Entry] = {
    list.tail.foldLeft[(Entry, List[Entry])]((list.head, List()))((z, x) =>
      if (cmp(z._1) < cmp(x)) (z._1, x :: z._2) else (x, z._1 :: z._2))._2
  }

  def dropSmallestV2(list: List[Entry], cmp: Entry => Float): List[Entry] = {
    val min_val = cmp(list.minBy(cmp))
    val (small, large) = list.partition(cmp(_) <= min_val)
    small.tail ++ large
  }

  def dropSmallestV3(list: List[Entry], cmp: Entry => Float): List[Entry] = {
    list.sortBy(cmp).tail
  }

  def apply(cursor: Stream[Entry], n: Int): List[Entry] = {
    var ret = cursor.take(n).toList
    for (file <- cursor.drop(n)) {
      /*
       * TODO will probably need to make sure it is GC friendly
       * if the stream is too large to fit in memory
       * c.f. Stream.foldLeft, Stream.reduceLeft
       */
      ret = dropSmallestV1(file :: ret, _._3)
    }
    ret
  }
}
