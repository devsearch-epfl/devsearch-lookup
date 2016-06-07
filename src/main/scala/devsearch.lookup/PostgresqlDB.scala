package devsearch.lookup

import java.sql.{Connection, DriverManager, ResultSet}

import scala.concurrent.{ExecutionContext, Future}

object PostgresqlDB {

  val FILE_PATH_KEY = "file_path"
  val CLUSTER_START_KEY = "cluster_start"
  val CLUSTER_END_KEY = "cluster_end"
  val SCORE_KEY = "score"
  val SIZE_SCORE_KEY = "size_score"
  val REPORANK_SCORE_KEY = "repo_rank"

  val db_protocol = "postgresql"

  //  TODO: set those parameters in config files
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

//    f"""
//        |SELECT * FROM (
//        |	SELECT
//        | $FILE_PATH_KEY%s,
//        |	($SIZE_SCORE_KEY%s*0.4)+($REPORANK_SCORE_KEY%s*0.4) AS $SCORE_KEY%s,
//        |	$CLUSTER_START_KEY%s,
//        | $CLUSTER_END_KEY%s,
//        | $SIZE_SCORE_KEY%s,
//        | $REPORANK_SCORE_KEY%s
//        |		FROM (
//        |		    SELECT
//        |           $FILE_PATH_KEY%s,
//        |		        clamp(size, 0, 100)/100 AS $SIZE_SCORE_KEY%s,
//        |		        $REPORANK_SCORE_KEY%s,
//        |		        $CLUSTER_START_KEY%s,
//        |		        $CLUSTER_END_KEY%s
//        |		    FROM (
//        |		        SELECT
//        |		            file AS $FILE_PATH_KEY%s,
//        |		            count(*) AS size,
//        |		            max(reporank) AS $REPORANK_SCORE_KEY%s,
//        |		            min(line) AS $CLUSTER_START_KEY%s,
//        |		            max(line) AS $CLUSTER_END_KEY%s
//        |		        FROM devsearch_features
//        |		        WHERE feature IN ($feature_list%s)
//        |		        GROUP BY file
//        |		        ) AS result
//        |		    ) AS scoring
//        |		) AS sorting ORDER BY score DESC
//        |LIMIT $len%d OFFSET $from%d
//      """.stripMargin

    f"""
       |SELECT
       |    files.file as $FILE_PATH_KEY%s,
       |    $SIZE_SCORE_KEY%s,
       |    $REPORANK_SCORE_KEY%s,
       |    $SCORE_KEY%s,
       |    $CLUSTER_END_KEY%s,
       |    $CLUSTER_START_KEY%s
       |FROM (
       |    SELECT * FROM (
       |        SELECT
       |            file,
       |            $SIZE_SCORE_KEY%s,
       |            $REPORANK_SCORE_KEY%s,
       |            ($SIZE_SCORE_KEY%s*0.4)+($REPORANK_SCORE_KEY%s*0.4) AS $SCORE_KEY%s,
       |            $CLUSTER_END_KEY%s,
       |            $CLUSTER_START_KEY%s
       |        FROM (
       |            SELECT
       |                file,
       |                clamp(count(*), 0, 100)/100 as $SIZE_SCORE_KEY%s,
       |                max(reporank) as $REPORANK_SCORE_KEY%s,
       |                min(line) AS $CLUSTER_END_KEY%s,
       |                max(line) AS $CLUSTER_START_KEY%s
       |            FROM data, features
       |            WHERE data.feature = features.id  AND features.feature IN ($feature_list%s)
       |            GROUP BY file
       |        ) AS scoring
       |    ) AS sorting
       |    ORDER BY score DESC
       |    LIMIT $len%d
       |    OFFSET $from%d
       |) as results, files
       |WHERE files.ID = results.file
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

    val finalQuery: String = fullQuery(features, languages, len, from)

    Future {
      try {
        // Configure to be Read Only
        val statement = db.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

        // Execute Query
        val rs: ResultSet = statement.executeQuery(finalQuery)

        var results: Set[SearchResultEntry] = Set()

        // Iterate Over returned results
        // TODO: Find a more "scala" way to eat results
        while (rs.next) {

          val filePath = rs.getString(FILE_PATH_KEY)

          // extract user and repo from filePath
          val firstSlash = filePath.indexOf("/")
          val secondSlash = filePath.indexOf("/", firstSlash + 1)

          val user = filePath.substring(0, firstSlash)
          val repo = filePath.substring(firstSlash + 1, secondSlash)
          val path = filePath.substring(secondSlash + 1)


          val start = rs.getInt(CLUSTER_START_KEY)
          val end = rs.getInt(CLUSTER_END_KEY)
          val score = rs.getFloat(SCORE_KEY)

          val scoreBreakDown: Map[String, Double] = Map(
            "final" ->  score,
            "size" -> 0.4 * rs.getDouble(SIZE_SCORE_KEY),
            "repoRank" -> 0.4 * rs.getDouble(REPORANK_SCORE_KEY)
          )

          // TODO: remove when intervals are more meaningfull
          results += SearchResultEntry(user, repo, path, start, Math.min(end, start + 10), score, scoreBreakDown, Set())
        }


        // TODO: get the actual count of results
        SearchResultSuccess(results.toSeq, 10);

      } catch  {
        case e: Exception=>
          e.printStackTrace()
          println(finalQuery);
          SearchResultError("Exception on the query " + e.getCause + " \n ")
      }
    }
  }
}
