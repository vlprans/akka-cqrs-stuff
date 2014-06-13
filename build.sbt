organization := "com.vlprans"

name := "cqrsexample"

version := "0.0.1"

scalaVersion := "2.11.1"

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io",
  "SpinGo OSS Releases" at "http://spingo-oss.s3.amazonaws.com/repository/releases"
)

libraryDependencies ++= Dependencies.backend

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

incOptions := incOptions.value.withNameHashing(true)

fork := true

Revolver.settings

initialCommands in console := """
  import scalaz._, Scalaz._
  import akka.actor._
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.{Future, Await}
  import scala.concurrent.duration._
  val system = ActorSystem("cqrs-example")
  implicit val timeout = Timeout(5 seconds)
"""
