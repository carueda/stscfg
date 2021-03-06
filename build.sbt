organization := "com.github.carueda"
name := "stscfg"
version := "0.0.1"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.8", "2.12.1")

libraryDependencies ++= Seq(
  "org.scala-lang"  % "scala-reflect"   % scalaVersion.value,
  "com.typesafe"    %   "config"        %  "1.3.1",
  "com.lihaoyi"    %%   "sourcecode"    %  "0.1.3",
  "com.lihaoyi"    %%   "utest"         %  "0.4.5"  % "test"
)
testFrameworks += new TestFramework("utest.runner.Framework")

coverageMinimum := 80
coverageFailOnMinimum := false
coverageHighlighting := { scalaBinaryVersion.value == "2.12" }

publishMavenStyle := true
publishArtifact in Test := false
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
pomIncludeRepository := { _ => false }
homepage := Some(url("https://github.com/carueda/stscfg"))
licenses := Seq("Apache 2.0" -> url("http://www.opensource.org/licenses/Apache-2.0"))
scmInfo := Some(ScmInfo(url("http://github.com/carueda/stscfg"), "scm:git@github.com:carueda/stscfg.git"))
pomExtra :=
  <developers>
    <developer>
      <id>carueda</id>
      <name>Carlos Rueda</name>
      <url>http://carueda.info</url>
    </developer>
  </developers>

sonatypeProfileName := "com.github.carueda"
