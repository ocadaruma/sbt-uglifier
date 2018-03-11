organization := "com.mayreh"

organizationName := "Haruki Okada"

name := "sbt-uglifier"

version := "1.0"

sbtVersion := "1.0.2"

libraryDependencies ++= Seq(
//  "org.scalariform" %% "scalariform" % "0.1.8",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.geirsson" %% "scalafmt-core" % "1.4.0"
)

sbtPlugin := true
