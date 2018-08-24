package codacy.duplication.phpcpd

import codacy.docker.api.Source
import codacy.docker.api.duplication.{DuplicationClone, DuplicationCloneFile}
import com.codacy.api.dtos.Languages
import org.specs2.mutable.Specification

import scala.util.{Failure, Success, Try}

class PHPCPDSpec extends Specification {

  private val expectedCloneLines =
    """    public function insert($item) {
      |        $node = new BinaryNode($item);
      |        if ($this->isEmpty()) {
      |            // special case if tree is empty
      |            $this->root = $node;
      |        }
      |        else {
      |            // insert the node somewhere in the tree starting at the root
      |            $this->insertNode($node, $this->root);
      |        }
      |        $node = new BinaryNode($item);
      |        if ($this->isEmpty()) {
      |            // special case if tree is empty
      |            $this->root = $node;
      |        }
      |        else {
      |            // insert the node somewhere in the tree starting at the root
      |            $this->insertNode($node, $this->root);
      |        }
      |    }
      |
      |
      |    protected function insertNode($node, &$subtree) {
      |        if ($subtree === null) {
      |            // insert node here if subtree is empty
      |            $subtree = $node;
      |        }
      |        else {
      |            if ($node->value > $subtree->value) {
      |                // keep trying to insert right
      |                $this->insertNode($node, $subtree->right);
      |            }
      |            else if ($node->value < $subtree->value) {
      |                // keep trying to insert left
      |""".stripMargin
  private val expectedDuplication = List(
    DuplicationClone(
      cloneLines = expectedCloneLines,
      nrTokens = 85,
      nrLines = 34,
      files = List(
        DuplicationCloneFile("codacy/duplication/php/FileWithDuplication1.php", 4, 38),
        DuplicationCloneFile("codacy/duplication/php/FileWithDuplication.php", 8, 42))))

  private val targetDir = "src/test/resources/"

  "PHPCPD" should {

    "get duplication results" in {

      val duplicationResults: Try[List[DuplicationClone]] =
        PHPCPD(path = Source.Directory(targetDir), language = Some(Languages.PHP), options = Map.empty)

      duplicationResults must beLike {
        case Success(duplication) =>
          duplication must beEqualTo(expectedDuplication)
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
}
