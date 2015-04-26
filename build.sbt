name := "devsearch-lookup"

shellPrompt := { state => "[\033[36m" + name.value + "\033[0m] $ " }

version := "0.1-snapshot"

scalaVersion := "2.10.4"
val akkaVersion = "2.3.9"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

/*resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)*/

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.7" % "test",
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion
  )

parallelExecution in Test := false
