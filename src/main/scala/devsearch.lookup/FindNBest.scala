package devsearch.lookup

import java.util.Random

/**
 * QuickSelect algorithm
 * http://en.wikipedia.org/wiki/Quickselect
 */
object FindNBestOld {
  val rand = new Random(System.currentTimeMillis())

  @deprecated(message = "use FindNBest instead", since = "2015-05-07")
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


object FindNBest {

  //TODO which version is fastest?

  def dropSmallestV1[Entry](list: List[Entry], cmp: Entry => Float): List[Entry] = {
    list.tail.foldLeft[(Entry, List[Entry])]((list.head, List()))((z, x) =>
      if (cmp(z._1) < cmp(x)) (z._1, x :: z._2) else (x, z._1 :: z._2))._2
  }

  def dropSmallestV2[Entry](list: List[Entry], cmp: Entry => Float): List[Entry] = {
    val min_val = cmp(list.minBy(cmp))
    val (small, large) = list.partition(cmp(_) <= min_val)
    small.tail ++ large
  }

  def dropSmallestV3[Entry](list: List[Entry], cmp: Entry => Float): List[Entry] = {
    list.sortBy(cmp).tail
  }

  def apply[Entry](stream: Stream[Entry], cmp: Entry => Float, n: Int): List[Entry] = {
    var ret = stream.take(n).toList
    for (file <- stream.drop(n)) {
      /*
       * TODO will probably need to make sure it is GC friendly
       * if the stream is too large to fit in memory
       * c.f. Stream.foldLeft, Stream.reduceLeft
       */
      ret = dropSmallestV1(file :: ret, cmp)
    }
    ret.sortBy(cmp).reverse
  }
}
