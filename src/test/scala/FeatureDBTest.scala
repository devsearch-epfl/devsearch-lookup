import devsearch.lookup.FeatureDB
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.MongodExecutable
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.config.IMongodConfig
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network

trait MongoTestDb extends BeforeAndAfterAll { this: Suite =>

  private var mongodExecutable: MongodExecutable = null

  override def beforeAll() {
    val starter: MongodStarter = MongodStarter.getDefaultInstance()

    val port = 27017
    val mongodConfig: IMongodConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net(port, Network.localhostIsIPv6()))
      .build()

    mongodExecutable = starter.prepare(mongodConfig)
    mongodExecutable.start()

    super.beforeAll() // To be stackable, must call super.beforeEach

    println("========== done setup")
  }

  override def afterAll() {
    try {
      super.afterAll() // To be stackable, must call super.afterEach
    }
    finally {
      if (mongodExecutable != null) {
        mongodExecutable.stop()
      }
    }
  }
}


class FeatureDBTest extends FlatSpec with Matchers with MongoTestDb {

  "The FeatureDB" should "return 2 times 5 hits" in {
    println("============ trying test")
    val results = FeatureDB.getMatchesFromDb(Seq("dummyfeature1", "dummyfeature2", "dummyfeature3", "dummyfeature4", "dummyfeature5"))
    val list = Await.result(results, Duration.Inf)

    list.size should be (2)
    list.map{ docHit => docHit.hits.length}.toList should be (List(5,5))
  }

  it should "return the 2 times 2 hits" in {
    val results = FeatureDB.getMatchesFromDb(Seq("dummyfeature1", "dummyfeature5"))
    val list = Await.result(results, Duration.Inf)

    list.size should be (2)
    list.map{ docHit => docHit.hits.length}.toList should be (List(2,2))
  }

  it should "return an empty stream when there is no match" in {
    val results = FeatureDB.getMatchesFromDb(Seq("dummy feature", "I won't match anything", "oh yeah baby", "this list is now long enough"))
    val list = Await.result(results, Duration.Inf)

    list.size should be (0)
  }
}
