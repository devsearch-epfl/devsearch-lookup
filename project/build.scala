import sbt._

object MyBuild extends Build{

  lazy val root = Project("root", file(".")).dependsOn(astProject).dependsOn(learningProject)
  lazy val astProject = RootProject(uri("git://github.com/devsearch-epfl/devsearch-ast.git#" + astProjectCommit))
  lazy val astProjectCommit = "8b028a9ac78b01680a22994b08aef1436ac1382b"
  lazy val learningProject = RootProject(uri("git://github.com/devsearch-epfl/devsearch-learning.git#" + learningProjectCommit))
  lazy val learningProjectCommit = "547a3642315c1afae4076409bbe5bf23fa8df73b"

}