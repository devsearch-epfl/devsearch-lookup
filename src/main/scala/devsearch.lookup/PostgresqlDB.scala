package devsearch.lookup

import java.sql.{Connection, DriverManager, ResultSet}

import scala.concurrent.{ExecutionContext, Future}

object PostgresqlDB {

  val db_protocol = "postgresql"

  //  TODO: sent those parameters to config files
  val db_host = "localhost"
  val db_port = 5432
  val db_name = "matt"

  // url to connect to database
  val db_url = "jdbc:" + db_protocol + "://" + db_host + ":" + db_port + "/" + db_name

  // lazy load the db
  // Note: passwords are not needed at the moment, we do not store sensitive information
  def db: Connection = DriverManager.getConnection(db_url)

  def fullQuery(features: Set[String], languages: Set[String], len: Int, from: Int): String = {

    // TODO: filter string to escape forbidden characters
    val feature_list: String = features.map("\'" + _ + "\'").mkString(",")

    f"""
        |SELECT * FROM (
        |	SELECT file,
        |	size,
        |	reporank,
        |	(size*0.4)+(reporank*0.4) AS score,
        |	maxline AS end,
        |	minline AS start
        |		FROM (
        |		    SELECT file,
        |		        clamp(size, 0, 100)/100 as size,
        |		        reporank,
        |		        minLine,
        |		        maxLine
        |		    FROM (
        |		        SELECT
        |		            file,
        |		            count(*) as size,
        |		            max(reporank) as reporank,
        |		            min(line) AS minLine,
        |		            max(line) AS maxLine
        |		        FROM devsearch_features
        |		        WHERE feature IN ($feature_list%s)
        |		        GROUP BY file
        |		        ) AS result
        |		    ) AS scoring
        |		) AS sorting ORDER BY score DESC
        |LIMIT $len%d OFFSET $from%d
      """.stripMargin
  }

  def simpleQuery() = {
    try {
      // Configure to be Read Only
      val statement = db.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      // Execute Query
      val rs = statement.executeQuery("SELECT * FROM devsearch_features LIMIT 5")

      // Iterate Over ResultSet
      while (rs.next) {
        println(rs.getString("file"))
      }
    }
  }

  def findBestFileMatches(features: Set[String], languages: Set[String], len: Int, from: Int)(implicit ec: ExecutionContext): Future[SearchResult] = {

    Future {
      try {
        // Configure to be Read Only
        val statement = db.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

        // Execute Query
        val rs: ResultSet = statement.executeQuery(fullQuery(features, languages, len, from))

        var results: Set[SearchResultEntry] = Set()

        // Iterate Over returned results
        // TODO: Find a more "scala" way to eat results
        while (rs.next) {

          // TODO: replace Hardcoded keys
          val filePath = rs.getString("file")

          // extract user and repo from filePath
          val firstSlash = filePath.indexOf("/")
          val secondSlash = filePath.indexOf("/", firstSlash + 1)

          val user = filePath.substring(0, firstSlash)
          val repo = filePath.substring(firstSlash + 1, secondSlash)
          val path = filePath.substring(secondSlash + 1)


          val start = rs.getInt("start")
          val end = rs.getInt("end")
          val score = rs.getFloat("score")

          val scoreBreakDown: Map[String, Double] = Map(
            "sizeScore" -> rs.getDouble("size"),
            "repoRank" -> rs.getDouble("reporank")
          )

          // TODO: remove when intervals are more meaningfull
          results += SearchResultEntry(user, repo, path, start, Math.min(end, start + 10), score, scoreBreakDown, Set())
        }


        // TODO: get the actual count of results
        SearchResultSuccess(results.toSeq, 10);

      } catch  {
        case e: Exception=>
          SearchResultError("Exception on the query" + e.getCause + "\n" + e.getStackTrace)
      }
    }
  }
}
