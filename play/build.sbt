name := """play-scala-starter-example"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)


resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.3"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test
libraryDependencies += "com.h2database" % "h2" % "1.4.194"


libraryDependencies += play.sbt.PlayImport.cacheApi
// include play-redis library
libraryDependencies += "com.github.karelcemus" %% "play-redis" % "1.5.1"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.9"
// https://mvnrepository.com/artifact/org.mockito/mockito-core
libraryDependencies += "org.mockito" % "mockito-core" % "2.9.0" % "test"
