import AssemblyKeys._  // put this at the top of the file

assemblySettings




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

// https://github.com/ReactiveMongo/ReactiveMongo#set-up-your-project-dependencies
resolvers += "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.7" % "test",
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23",
  "org.apache.spark" %% "spark-core" % "1.3.0" % "provided",
  "io.spray" %%  "spray-json" % "1.3.1"
)

//sources in Compile <<= (sources in Compile).map(_ filter(_.name == "devsearch.util.DateSplitter.scala"))

Revolver.settings

parallelExecution in Test := false
