package codacy.duplication.phpcpd

import com.codacy.plugins.api.Source
import com.codacy.plugins.api.duplication.{DuplicationClone, DuplicationCloneFile}
import com.codacy.plugins.api.languages.Languages
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import scala.util.{Failure, Success, Try}

class PHPCPDSpec extends Specification {

  private val targetDir = "src/test/resources/"

  val expectedDuplication = Seq(
    DuplicationClone(
      "",
      113,
      27,
      List(
        DuplicationCloneFile("codacy/duplication/php/FileWithDuplication1.php", 131, 158),
        DuplicationCloneFile("codacy/duplication/php/FileWithDuplication.php", 69, 96))),
    DuplicationClone(
      "",
      92,
      23,
      List(
        DuplicationCloneFile("codacy/duplication/php/FileWithDuplication1.php", 68, 91),
        DuplicationCloneFile("codacy/duplication/php/FileWithDuplication.php", 107, 130)))).sortBy(_.nrTokens)

  "PHPCPD" should {

    "get duplication results" in {

      val duplicationResults: Try[List[DuplicationClone]] =
        PHPCPD(path = Source.Directory(targetDir), language = Some(Languages.PHP), options = Map.empty)

      duplicationResults must beLike {
        case Success(duplication) =>
          val clonesWithoutLines = duplication.map(_.copy(cloneLines = "")).sortBy(_.nrTokens)

          clonesWithoutLines must have size 2

          testClone(clonesWithoutLines(0), expectedDuplication(0))
          testClone(clonesWithoutLines(1), expectedDuplication(1))
      }

    }

    "fail" in {
      "wrong language" in {
        val duplicationResults: Try[List[DuplicationClone]] =
          PHPCPD(path = Source.Directory(targetDir), language = Some(Languages.Scala), options = Map.empty)

        duplicationResults must beLike {
          case Failure(e) =>
            e.getMessage must beEqualTo(s"PHPCPD only supports PHP. Provided language: ${Languages.Scala.name}")
        }
      }
    }

  }

  private def testClone(clone: DuplicationClone,
                        expectedClone: DuplicationClone): MatchResult[Seq[DuplicationCloneFile]] = {
    clone.nrLines must beEqualTo(expectedClone.nrLines)
    clone.nrTokens must beEqualTo(expectedClone.nrTokens)
    clone.files must containTheSameElementsAs(expectedClone.files)
  }

}
