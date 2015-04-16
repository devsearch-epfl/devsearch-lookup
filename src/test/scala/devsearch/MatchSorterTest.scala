package devsearch

import org.scalatest._
import utils.SparkTestUtils._

class MatchSorterTest extends FlatSpec {

  "MatchSorter" should "sort correctly" in withSpark(sc => {
    val features = sc.parallelize(List(
      "file1" -> Iterable(
        FeatureData("variableDeclaration=i type=int", "user1", "repo1", "some/other/dir", "File1.scala", 10)
      ),
      "file2" -> Iterable(
        FeatureData("variableDeclaration=i type=int", "user2", "repo2", "some/dir", "File2.scala", 4),
        FeatureData("variableDeclaration=x type=int", "user2", "repo2", "some/dir", "File2.scala", 5)
      )
    ))

    val results = MatchSorter.sort(features).collect()
    val expected = Seq("file2", "file1")

    results.zip(expected).foreach{case (actual, expected) => assert(actual == expected)}
  })
}
