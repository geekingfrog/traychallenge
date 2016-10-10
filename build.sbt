name := "Tray.io technical test"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.11"
  , "com.typesafe.akka" %% "akka-http-experimental" % "2.4.11"
  , "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.11"
  , "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test"
  , "com.typesafe.akka" %% "akka-testkit" % "2.4.11"
  , "com.typesafe.akka" %% "akka-http-testkit" % "2.4.11"
)
