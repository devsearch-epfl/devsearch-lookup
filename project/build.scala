import sbt._

object MyBuild extends Build{

  lazy val root = Project("root", file(".")).dependsOn(astProject)
  lazy val astProject = RootProject(uri("git://github.com/devsearch-epfl/devsearch-ast.git#" + astProjectCommit))
  lazy val astProjectCommit = "cdffdba2bc77e344eafa0252ccbd04d299933243"

}