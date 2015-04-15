name := "devsearch-lookup"

version := "0.1-snapshot"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

/*resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)*/

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.7" % "test",
  "org.apache.spark" %% "spark-core" % "1.3.0"
)

parallelExecution in Test := false
