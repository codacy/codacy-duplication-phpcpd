package codacy.duplication.phpcpd

import java.io.File
import java.nio.file.{Files, Path, Paths}

import codacy.dockerApi.api.{DuplicationClone, DuplicationCloneFile, DuplicationConfiguration}
import codacy.dockerApi.traits.IDuplicationImpl
import codacy.dockerApi.utils.CommandRunner

import scala.util.{Failure, Properties, Success, Try}
import scala.xml.{Elem, XML}

object PHPCPD extends IDuplicationImpl {

  override def apply(rootPath: Path, config: DuplicationConfiguration): Try[List[DuplicationClone]] = {
    val rootDirectory = rootPath.toFile
    val temporaryFile = Files.createTempFile("codacy-", ".xml").toFile
    CommandRunner.exec(getCommand(rootDirectory, temporaryFile), Some(rootDirectory)) match {
      case Right(resultFromTool) =>
        parseToolResult(rootDirectory, temporaryFile) match {
          case s@Success(_) => s
          case Failure(e) =>
            val msg =
              s"""
                 |PHPCPD exited with code ${resultFromTool.exitCode}
                 |message: ${e.getMessage}
                 |stdout: ${resultFromTool.stdout.mkString(Properties.lineSeparator)}
                 |stderr: ${resultFromTool.stderr.mkString(Properties.lineSeparator)}
                 |StackTrace: ${e.getStackTrace.mkString(System.lineSeparator)}
                """.stripMargin
            Failure(new Exception(msg))
        }

      case Left(e) =>
        Failure(e)
    }
  }

  private def parseToolResult(rootDirectory: File, temporaryFile: File): Try[List[DuplicationClone]] = {
    Try(XML.loadFile(temporaryFile)).flatMap(parseXml(rootDirectory, _))
  }

  private def getCommand(rootDirectory: File, outputFile: File): List[String] = {
    List("php", "-d", "memory_limit=-1", "/bin/phpcpd", "--log-pmd", outputFile.getCanonicalPath, rootDirectory.getCanonicalPath)
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
