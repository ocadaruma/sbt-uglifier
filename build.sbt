organization := "com.mayreh"

organizationName := "Haruki Okada"

name := "sbt-uglifier"

version := "1.0"

libraryDependencies ++= Seq(
  "org.scalariform" %% "scalariform" % "0.1.8",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

sbtPlugin := true
