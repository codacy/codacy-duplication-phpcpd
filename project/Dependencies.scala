import sbt._

object Dependencies {

  val scalaBinaryVersion = "2.12"
  val scalaVersion = s"$scalaBinaryVersion.4"

  val playJson = "com.typesafe.play" %% "play-json" % "2.6.9" withSources ()
  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.1.0" withSources ()

  object Codacy {
    val duplicationScalaSeed = "com.codacy" %% "codacy-duplication-scala-seed" % "2.0.0-pre.151"
  }
  val specs2Version = "4.3.2"
  val specs2 = "org.specs2" %% "specs2-core" % specs2Version

}
