import sbt._


object Library {
  val akkaVersion = "2.3.2"
  val sprayVersion = "1.3.1-20140423"
  val scalazVersion = "7.1.0-M7"

  val scalaz =           "org.scalaz"        %% "scalaz-core"                    % scalazVersion
  val akkaActor =        "com.typesafe.akka" %% "akka-actor"                     % akkaVersion
  val akkaPersistence =  "com.typesafe.akka" %% "akka-persistence-experimental"  % akkaVersion
  val akkaRemote =       "com.typesafe.akka" %% "akka-remote"                    % akkaVersion
  val scalaStm =         "org.scala-stm"     %% "scala-stm"                      % "0.7"
  val akkaSlf4j =        "com.typesafe.akka" %% "akka-slf4j"                     % akkaVersion
  val akkaTestkit =      "com.typesafe.akka" %% "akka-testkit"                   % akkaVersion
  val scalatest =        "org.scalatest"     %% "scalatest"                      % "2.1.6"
}

object Dependencies {
  import Library._

  val backend = Seq(
    scalaz,
    akkaActor,
    akkaRemote,
    akkaPersistence,
    scalaStm,
    akkaSlf4j,
    scalatest % "test",
    akkaTestkit % "test"
  )
}
