package devsearch

import org.scalatest.{FlatSpec, Matchers}
import utils.SparkTestUtils._

class FeatureRetrieverTest extends FlatSpec with Matchers {

  Config.featuresPath = "./testData/smallFeature"



    "FeatureRetriever" should "return an emptyList" in withSpark(implicit sc => {

      val l = FeatureRetriever.get(Seq())
      val l2 = FeatureRetriever.get(List("variableDeclaration=rest type=devsearch.ast.PrimitiveTypes.Int$"))

      assert(l.count() == 0)
      assert(l2.count() == 2)
    }
  )

}
