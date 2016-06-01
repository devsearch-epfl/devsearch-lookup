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
  "com.github.scopt" %% "scopt" % "3.3.0",
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23",

  // http://mvnrepository.com/artifact/org.postgresql/postgresql
  "org.postgresql" % "postgresql" % "9.4.1208.jre7"
)


Revolver.settings

parallelExecution in Test := false

target in Compile in doc := baseDirectory.value / "api"
