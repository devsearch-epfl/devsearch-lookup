import devsearch.lookup.FeatureDB
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class ExampleSpec extends FlatSpec with Matchers {

  "The FeatureDB" should "return the correct amount of result" in {
    val results = FeatureDB.getMatchesFromDb(Seq("className=ExampleModule", "typeReference=Type", "variableDeclaration=connectorId type=String", "variableDeclaration=type type=Type"))
    val list = Await.result(results, Duration.Inf)

    list.size should be (10)
  }

}