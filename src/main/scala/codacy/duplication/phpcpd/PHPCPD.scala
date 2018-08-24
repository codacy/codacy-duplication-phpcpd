package codacy.duplication.phpcpd

import java.io.File
import java.nio.file.Paths

import codacy.docker.api.{DuplicationConfiguration, Source}
import codacy.docker.api.duplication.{DuplicationClone, DuplicationCloneFile, DuplicationTool}
import codacy.docker.api.utils.{CommandResult, CommandRunner}
import com.codacy.api.dtos.{Language, Languages}

import scala.util.{Failure, Properties, Try}
import scala.xml.{Elem, XML}

object PHPCPD extends DuplicationTool {

  def apply(path: Source.Directory,
            language: Option[Language],
            options: Map[DuplicationConfiguration.Key, DuplicationConfiguration.Value]): Try[List[DuplicationClone]] = {
    language match {
      case Some(lang) if lang != Languages.PHP =>
        Failure(new Exception(s"PHPCPD only supports PHP. Provided language: ${lang.name}"))
      case _ =>
        val rootDirectory = better.files.File(path.path).toJava
        withOutputFile { outputFile: File =>
          runTool(rootDirectory, outputFile).flatMap { resultFromTool =>
            parseToolResult(rootDirectory, outputFile).recoverWith {
              case e => handleFailure(resultFromTool, e)
            }
          }
        }
    }
  }

  private def runTool(rootDirectory: File, outputFile: File): Try[CommandResult] = {
    getCommand(rootDirectory, outputFile).flatMap { command =>
      CommandRunner.exec(command, Option(rootDirectory)).toTry
    }
  }

  private def parseToolResult(rootDirectory: File, temporaryFile: File): Try[List[DuplicationClone]] = {
    Try(XML.loadFile(temporaryFile)).flatMap(parseXml(rootDirectory, _))
  }

  private def getCommand(rootDirectory: File, outputFile: File): Try[List[String]] = {
    (for {
      whichOutput <- CommandRunner.exec(List("which", "phpcpd"))
      phpCpdLocation <- whichOutput.stdout.headOption.toRight(new Throwable("phpcpd is not available in $PATH"))
    } yield {
      List(
        "php",
        "-d",
        "memory_limit=-1",
        phpCpdLocation,
        "--log-pmd",
        outputFile.getCanonicalPath,
        rootDirectory.getCanonicalPath)
    }).toTry
  }

  private def parseXml(rootDirectory: File, elem: Elem): Try[List[DuplicationClone]] = {
    Try {
      (elem \ "duplication").map { duplication =>
        val tokens = (duplication \ "@tokens").text.toInt
        val lines = (duplication \ "@lines").text.toInt

        val files = (duplication \ "file").map { file =>
          val filePath = (file \ "@path").text
          val relativePath =
            rootDirectory.toPath.relativize(Paths.get(filePath)).toString
          val line = (file \ "@line").text.toInt

          DuplicationCloneFile(relativePath, line, line + lines)
        }

        val code = (duplication \ "codefragment").text
        DuplicationClone(code, tokens, lines, files)
      }(collection.breakOut)
    }
  }

  private def withOutputFile[T](fn: File => T): T = {
    (for {
      file <- better.files.File.temporaryFile(suffix = ".xml")
    } yield {
      fn(file.toJava)
    }).get()
  }

  private def handleFailure(resultFromTool: CommandResult, e: Throwable) = {
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

}
