import devsearch.lookup.{Location, RankingDB}
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class RankingDBTest extends FlatSpec with Matchers {

  "The RankingDB" should "return 0 when the repo has no rank" in {
    val result = RankingDB.getRanking(Location("NoexistentOwner", "NoexistentRepo", "this/part/is/useless.java"))
    val score = Await.result(result, Duration.Inf)

    score should be (0)
  }

  it should "return return the correct result when there is a match" in {

    val locs = List(Location("testOwner1", "testRepo1", "this/part/is/useless.java"),
      Location("the-dotyuofs", "the-grid", "this/part/is/useless.java"))

    val results = locs.map(RankingDB.getRanking)
    val scores = results.map(Await.result(_, Duration.Inf))

    scores should be (List(0.1122334455, 46.97983855456367))
  }

  it should "return one of the value when there is several matches" in {
    val result = RankingDB.getRanking(Location("testOwner2", "testRepo2", "this/part/is/useless.java"))
    val score = Await.result(result, Duration.Inf)

    List(0.2829776507907872, 0.4374722892348062, 1.6340852979580627) should contain (score)
//    score should be oneOf List(0.2829776507907872, 0.4374722892348062, 1.6340852979580627)
  }
}