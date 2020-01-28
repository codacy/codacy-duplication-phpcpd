import sbt.Keys._
import sbt._

name := """codacy-duplication-phpcpd"""

version := "1.0.0-SNAPSHOT"

scalaVersion := Dependencies.scalaVersion
scalaVersion in ThisBuild := Dependencies.scalaVersion
scalaBinaryVersion in ThisBuild := Dependencies.scalaBinaryVersion

scapegoatVersion in ThisBuild := "1.3.5"

lazy val codacyDuplicationPHPCPD = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    inThisBuild(
      List(
        organization := "com.codacy",
        scalaVersion := Dependencies.scalaVersion,
        version := "0.1.0-SNAPSHOT",
        resolvers := Seq("Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/releases")) ++ resolvers.value,
        scalacOptions ++= Common.compilerFlags,
        scalacOptions in Test ++= Seq("-Yrangepos"),
        scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"))),
    // App Dependencies
    libraryDependencies ++= Seq(Dependencies.Codacy.duplicationScalaSeed, Dependencies.playJson, Dependencies.scalaXml),
    // Test Dependencies
    libraryDependencies ++= Seq(Dependencies.specs2).map(_ % Test))
  .settings(Common.dockerSettings: _*)
