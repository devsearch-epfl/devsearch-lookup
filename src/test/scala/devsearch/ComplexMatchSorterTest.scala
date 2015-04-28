package devsearch

import org.apache.spark.SparkContext
import org.scalatest._
import utils.SparkTestUtils._

class ComplexMatchSorterTest extends FlatSpec {

  def getFeaturesByLocation()(implicit sc: SparkContext) = {
    val features = List(
      FeatureData("variableDeclaration=i type=int", "user1", "repo1", "some/other/dir/File1.scala", 10),
      FeatureData("variableDeclaration=i type=int", "user2", "repo2", "some/dir/File2.scala", 4),
      FeatureData("variableDeclaration=x type=int", "user2", "repo2", "some/dir/File2.scala", 5)
    )

    val featuresByLocation = sc.parallelize(features).groupBy { f =>
      Location(f.user + "/" + f.repo, f.path)
    }

    featuresByLocation
  }

  "MatchSorter" should "sort correctly" in withSpark(implicit sc => {
    val featuresByLocation = getFeaturesByLocation()

    val results = ComplexMatchSorter.sort(featuresByLocation, withRanking = false).toList
    println(results)
    val expected = Seq("file2", "file1")

    assert(results == List(
      (Location("user2/repo2", "some/dir/File2.scala"), 4),
      (Location("user1/repo1", "some/other/dir/File1.scala"), 10))
    )
  })

  it should "return only as many entries as we ask it for" in withSpark(implicit sc => {
    val featuresByLocation = getFeaturesByLocation()

    val results = ComplexMatchSorter.sort(featuresByLocation, withRanking = false, numToReturn = 1).toList

    assert(results.length == 1)
  })
}
