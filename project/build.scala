import sbt._

object MyBuild extends Build{

  lazy val root = Project("root", file(".")).dependsOn(astProject)
  lazy val astProject = RootProject(uri("git://github.com/devsearch-epfl/devsearch-ast.git#" + astProjectCommit))
  lazy val astProjectCommit = "8b028a9ac78b01680a22994b08aef1436ac1382b"

}