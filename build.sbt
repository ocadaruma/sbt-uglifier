organization := "com.mayreh"

organizationName := "Haruki Okada"

name := "sbt-uglifier"

version := "1.0"

crossSbtVersions := Seq("0.13.15", "1.0.2")

sbtPlugin := true

libraryDependencies ++= Seq(
  "org.scalariform" %% "scalariform" % "0.2.6",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

licenses += (("MIT", url("https://raw.githubusercontent.com/ocadaruma/sbt-uglifier/master/LICENSE")))

startYear := Some(2018)

publishMavenStyle := true

pomExtra in Global := {
  <url>https://github.com/ocadaruma/sbt-uglifier</url>
    <scm>
      <connection>"scm:git:git@github.com:ocadaruma/sbt-uglifier.git"</connection>
      <url>git@github.com:ocadaruma/sbt-uglifier.git</url>
    </scm>
    <developers>
      <developer>
        <id>ocadaruma</id>
        <name>Haruki Okada</name>
      </developer>
    </developers>
}
