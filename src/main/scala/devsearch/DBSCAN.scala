package devsearch

/**
 * Textbook implementation of the DBSCAN algorithm, a clustering algorithm that doesn't require
 * knowing the number of clusters before-hand.
 *
 * @see http://en.wikipedia.org/wiki/DBSCAN
 */
object DBSCAN {

  def apply(points: Array[Int], epsilon: Double, minPoints: Int) = {
    var c = 0

    val noise = scala.collection.mutable.Set.empty[Int]
    val visited = scala.collection.mutable.Set.empty[Int]
    val clustered = scala.collection.mutable.Set.empty[Int]
    val clusters = scala.collection.mutable.Map.empty[Int, Set[Int]].withDefaultValue(Set.empty)

    def regionQuery(i: Int) = {
      def takeWhile(start: Int, inc: Int, p: Int => Boolean): List[Int] = {
        if (start >= 0 && start < points.length && p(points(start)))
          start :: takeWhile(start + inc, inc, p)
        else Nil
      }
      val value = points(i)
      val predicate = (p: Int) => Math.abs(p - value) <= epsilon
      takeWhile(i, -1, predicate) ++ takeWhile(i+1, +1, predicate)
    }

    def expandCluster(i: Int, neighbors: List[Int], c: Int) = {
      clustered += i
      clusters(c) += points(i)

      var allNeighbors = neighbors
      var n = 0
      while (n < allNeighbors.size) {
        val j = allNeighbors(n)
        if (!visited(j)) {
          visited += j
          val jNeighbors = regionQuery(j)
          if (jNeighbors.size >= minPoints) {
            allNeighbors ++= jNeighbors.filterNot(allNeighbors.contains(_))
          }
        }

        if (!clustered(j)) {
          clusters(c) += points(j)
        }

        n += 1
      }
    }

    for (i <- 0 to points.length - 1 if !visited(i)) {
      visited += i
      val neighbors = regionQuery(i)
      if (neighbors.size < minPoints)
        noise += i
      else {
        c += 1
        expandCluster(i, neighbors, c)
      }
    }

    clusters.values ++ noise.map(i => Set(points(i)))
  }
}
