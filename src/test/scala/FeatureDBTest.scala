import devsearch.lookup.FeatureDB
import org.scalatest._

class ExampleSpec extends FlatSpec with Matchers {

  "The FeatureDB" should "return the correct amount of result" in {
    val results = FeatureDB.getMatchesFromDb(Seq("className=ExampleModule", "typeReference=Type", "variableDeclaration=connectorId type=String", "variableDeclaration=type type=Type"))
    whenReady(results) {
      list => list.
    }
  }

}