package devsearch.lookup

object FindNBest {

  //TODO which version is fastest?

  def dropSmallestV1[Entry](list: List[Entry], scoreFunc: Entry => Float): List[Entry] = {
    list.tail.foldLeft[(Entry, List[Entry])]((list.head, List()))((z, x) =>
      if (scoreFunc(z._1) < scoreFunc(x)) (z._1, x :: z._2) else (x, z._1 :: z._2))._2
  }

  def dropSmallestV2[Entry](list: List[Entry], scoreFunc: Entry => Float): List[Entry] = {
    val min_val = scoreFunc(list.minBy(scoreFunc))
    val (small, large) = list.partition(scoreFunc(_) <= min_val)
    small.tail ++ large
  }

  def dropSmallestV3[Entry](list: List[Entry], scoreFunc: Entry => Float): List[Entry] = {
    list.sortBy(scoreFunc).tail
  }

  def apply[Entry](stream: Stream[Entry], scoreFunc: Entry => Float, n: Int): (List[Entry], Long) = {
    var ret = stream.take(n).toList
    var count = ret.size
    for (file <- stream.drop(n)) {
      /*
       * TODO will probably need to make sure it is GC friendly
       * if the stream is too large to fit in memory
       * c.f. Stream.foldLeft, Stream.reduceLeft
       */
      ret = dropSmallestV1(file :: ret, scoreFunc)
      count += 1
    }
    (ret.sortBy(scoreFunc).reverse, count)
  }
}
