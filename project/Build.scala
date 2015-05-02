import sbt._


// required for DataSplitter.
object MyBuild extends Build {

  lazy val root = Project("root", file(".")).dependsOn(HashRing)
                                            .dependsOn(devsearch_ast)

  lazy val HashRing = RootProject(uri("git://github.com/JosephMoniz/scala-hash-ring.git"))

  lazy val astCommit = "af4a968fa551b186f4c4f5eb6f9db60432da2826"
  lazy val devsearch_ast = RootProject(uri("git://github.com/devsearch-epfl/devsearch-ast.git#" + astCommit))

}
