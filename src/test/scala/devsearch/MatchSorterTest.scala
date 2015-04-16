package devsearch

import org.scalatest._
import utils.SparkTestUtils._

class MatchSorterTest extends FlatSpec {

  "MatchSorter" should "sort correctly" in withSpark(implicit sc => {

    val features = List(
      FeatureData("variableDeclaration=i type=int", "user1", "repo1", "some/other/dir", "File1.scala", 10),
      FeatureData("variableDeclaration=i type=int", "user2", "repo2", "some/dir", "File2.scala", 4),
      FeatureData("variableDeclaration=x type=int", "user2", "repo2", "some/dir", "File2.scala", 5)
    )

    val featuresByLocation = sc.parallelize(features).groupBy { f =>
      Location(f.user + "/" + f.repo, f.dir + "/" + f.file)
    }

    val results = MatchSorter.sort(featuresByLocation, withRanking = false).collect().toList
    println(results.toList)
    val expected = Seq("file2", "file1")

    assert(results == List(
      (Location("user2/repo2","some/dir/File2.scala"),4),
      (Location("user1/repo1","some/other/dir/File1.scala"),10))
    )
  })
}
