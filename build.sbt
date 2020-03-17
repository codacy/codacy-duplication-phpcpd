import com.typesafe.sbt.packager.docker.Cmd

name := "codacy-duplication-phpcpd"

val scala213 = "2.13.1"

scalaVersion := scala213
ThisBuild / scalaVersion := scala213
ThisBuild / scalaBinaryVersion := scala213.split('.').take(2).mkString(".")
ThisBuild / scapegoatVersion := "1.4.1"
ThisBuild / organization := "com.codacy"

addCompilerPlugin(scalafixSemanticdb)
scalacOptions ++= Seq("-Yrangepos", "-Ywarn-unused")

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.7.4",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  "com.codacy" %% "codacy-duplication-scala-seed" % "2.0.1",
  "org.specs2" %% "specs2-core" % "4.8.0" % Test)

val defaultDockerInstallationPath = "/opt/codacy"

val phpCPDVersion: String = scala.io.Source.fromFile(".phpcpd-version").mkString.trim

val installPHPCPD: String =
  s"""|export COMPOSER_HOME=/home/docker/.composer &&
      |mkdir -p /home/docker/.composer &&
      |chown -R docker:docker /home/docker &&
      |apk --update --no-cache add openjdk8-jre bash curl git php5 php5-xml php5-cli php5-pdo php5-curl php5-json php5-phar php5-ctype php5-openssl php5-dom &&
      |curl -sS https://getcomposer.org/installer | php -- --install-dir=/usr/bin --filename=composer &&
      |su - docker -c 'composer global require "sebastian/phpcpd=$phpCPDVersion"' &&
      |rm -rf /usr/bin/composer &&
      |rm -rf /tmp/* &&
      |rm -rf /var/cache/apk/*""".stripMargin.replaceAll(System.lineSeparator(), " ")

Docker / packageName := packageName.value
Docker / version := version.value
Docker / maintainer := "Codacy <team@codacy.com>"
dockerBaseImage := "library/php:5.6-alpine3.8"
dockerUpdateLatest := true
Docker / defaultLinuxInstallLocation := defaultDockerInstallationPath
Docker / daemonUser := "docker"
dockerEntrypoint := Seq(s"$defaultDockerInstallationPath/bin/${name.value}")
dockerCmd := Seq()
dockerCommands := dockerCommands.value.flatMap {
  case cmd @ Cmd("ADD", _) =>
    Seq(
      Cmd("ENV", "PATH /home/docker/.composer/vendor/bin:$PATH"),
      cmd,
      Cmd("RUN", s"mv $defaultDockerInstallationPath/docs /docs"),
      Cmd("RUN", installPHPCPD))

  case cmd @ Cmd("WORKDIR", _) =>
    Seq(Cmd("RUN", "adduser -u 2004 -D docker"), cmd)
  case other => Seq(other)
}

(Universal / mappings) ++= (Compile / resourceDirectory).map { resourceDir: File =>
  val src = resourceDir / "docs"
  val dest = "/docs"

  val docFiles = for {
    path <- src.allPaths.get if !path.isDirectory
  } yield path -> path.toString.replaceFirst(src.toString, dest)

  docFiles
}.value
