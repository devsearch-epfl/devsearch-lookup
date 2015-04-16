package devsearch

import org.scalatest.{Matchers, FlatSpec}

class FeatureIndexParserTest extends FlatSpec with Matchers{

  "A parser" should "parse a simple example" in {
    val feat = FeatureIndexParser.parse(FeatureIndexParser.featureEntry, "featKey,user,repo,file/hello.java,12")

    feat should not be 'empty

    feat.get should equal(FeatureData("featKey","user","repo","file/hello.java",12))

  }

  it should "parse a complex example" in {
    val feat = FeatureIndexParser.parse(FeatureIndexParser.featureEntry, "hello feature 232435\"*ç%%/ç%68689''''^???^âlsdf:_---\\,\\,ll.él§§°°\"+*%\"*ç577,user,repo,file,999999")

    feat should not be 'empty

    feat.get should equal(FeatureData("hello feature 232435\"*ç%%/ç%68689''''^???^âlsdf:_---\\,\\,ll.él§§°°\"+*%\"*ç577","user","repo","file",999999))
  }
}
