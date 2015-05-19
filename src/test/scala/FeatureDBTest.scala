import devsearch.lookup.FeatureDB
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class FeatureDBTest extends FlatSpec with Matchers {

  "The FeatureDB" should "return 2 times 5 hits" in {
    val results = FeatureDB.getMatchesFromDb(Set("dummyfeature1", "dummyfeature2", "dummyfeature3", "dummyfeature4", "dummyfeature5"), Set(), Set())
    val list = Await.result(results, Duration.Inf)

    list.size should be (2)
    list.map{ docHit => docHit.hits.length}.toList should be (List(5,5))
  }

  it should "return the 2 times 2 hits" in {
    val results = FeatureDB.getMatchesFromDb(Set("dummyfeature1", "dummyfeature5"), Set(), Set())
    val list = Await.result(results, Duration.Inf)

    list.size should be (2)
    list.map{ docHit => docHit.hits.length}.toList should be (List(2,2))
  }

  it should "return an empty stream when there is no match" in {
    val results = FeatureDB.getMatchesFromDb(Set("dummy feature", "I won't match anything", "oh yeah baby", "this list is now long enough"), Set(), Set())
    val list = Await.result(results, Duration.Inf)

    list.size should be (0)
  }

  it should "return an empty stream when no language match" in {
    val results = FeatureDB.getMatchesFromDb(Set("featureforlanguage1", "featureforlanguage2"), Set(), Set("Go"))
    val list = Await.result(results, Duration.Inf)

    list.size should be (0)
  }

  it should "return the correct amount of result for java and scala" in {
    val results = FeatureDB.getMatchesFromDb(Set("featureforlanguage1", "featureforlanguage2"), Set(), Set("Java", "JavaScript"))
    val list = Await.result(results, Duration.Inf)

    list.size should be (2)
  }

  it should "should give the correct repoRank" in {
    val results = FeatureDB.getMatchesFromDb(Set("dummyfeature1", "dummyfeature2"), Set(), Set("Java", "JavaScript"))
    val list = Await.result(results, Duration.Inf)

    list.map{ docHit => docHit.repoRank}.toList.sorted should be (List(0.598764,25266.234))
  }
}