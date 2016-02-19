package codacy.duplication.phpcpd

import java.io.File
import java.nio.file.{Files, Path, Paths}

import codacy.dockerApi.api.{DuplicationClone, DuplicationCloneFile, DuplicationConfiguration}
import codacy.dockerApi.traits.IDuplicationImpl
import codacy.dockerApi.utils.CommandRunner

import scala.util.Try
import scala.xml.{Elem, XML}

object PHPCPD extends IDuplicationImpl {

  override def apply(rootPath: Path, config: DuplicationConfiguration): Try[List[DuplicationClone]] = {
    val rootDirectory = rootPath.toFile
    val temporaryFile = Files.createTempFile("codacy-", ".xml").toFile
    CommandRunner.exec(getCommand(rootDirectory, temporaryFile), Some(rootDirectory))

    Try(XML.loadFile(temporaryFile)).flatMap(parseXml(rootDirectory, _))
  }

  private def getCommand(rootDirectory: File, outputFile: File): List[String] = {
    List("phpcpd", "--log-pmd", outputFile.getCanonicalPath, rootDirectory.getCanonicalPath)
  }

  private def parseXml(rootDirectory: File, elem: Elem): Try[List[DuplicationClone]] = {
    Try {
      (elem \ "duplication").map { duplication =>
        val tokens = (duplication \ "@tokens").text.toInt
        val lines = (duplication \ "@lines").text.toInt

        val files = (duplication \ "file").map { file =>
          val filePath = (file \ "@path").text
          val relativePath = rootDirectory.toPath.relativize(Paths.get(filePath)).toString
          val line = (file \ "@line").text.toInt

          DuplicationCloneFile(relativePath, line, line + lines)
        }

        val code = (duplication \ "codefragment").text
        DuplicationClone(code, tokens, lines, files)
      }.toList
    }
  }

}
