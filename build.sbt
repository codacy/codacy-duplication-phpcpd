import com.typesafe.sbt.packager.docker.Cmd

name := """codacy-duplication-phpcpd"""

version := "1.0.0-SNAPSHOT"

scalaVersion := Dependencies.scalaVersion

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"
)

libraryDependencies ++= Seq(
  Dependencies.playJson,
  Dependencies.scalaXml,
  Dependencies.duplicationScalaSeed,
  Dependencies.specs2 % Test
)

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

mappings.in(Universal) ++= resourceDirectory.in(Compile).map { (resourceDir: File) =>
  val src = resourceDir / "docs"
  val dest = "/docs"

  (for {
    path <- better.files.File(src.toPath).listRecursively()
    if !path.isDirectory
  } yield path.toJava -> path.toString.replaceFirst(src.toString, dest)).toSeq
}.value

val dockerUser = "docker"
val dockerGroup = "docker"

daemonUser in Docker := dockerUser

daemonGroup in Docker := dockerGroup

dockerBaseImage := "library/openjdk:8-jre-alpine"

val installAll =
  s"""
     |export COMPOSER_HOME=/opt/composer &&
     |mkdir -p $$COMPOSER_HOME &&
     |apk update &&
     |apk add bash curl git php5 php5-xml php5-cli php5-pdo php5-curl php5-json php5-phar php5-ctype php5-openssl php5-dom &&
     |ln -s /usr/bin/php5 /usr/bin/php &&
     |curl -sS https://getcomposer.org/installer | php -- --install-dir=/bin --filename=composer &&
     |composer global require "sebastian/phpcpd=2.0.1" &&
     |chmod -R 777 /opt &&
     |ln -s /opt/composer/vendor/bin/phpcpd /bin/phpcpd
   """.stripMargin.replaceAll(System.lineSeparator(), " ")

dockerCommands := dockerCommands.value.flatMap {
  case cmd@Cmd("WORKDIR", _) => List(cmd,
    Cmd("RUN", installAll)
  )

  case cmd@(Cmd("ADD", "opt /opt")) => List(cmd,
    Cmd("RUN", "adduser -u 2004 -D docker")
  )
  case other => List(other)
}
