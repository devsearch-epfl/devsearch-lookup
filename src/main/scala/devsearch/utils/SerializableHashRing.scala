package devsearch.utils

/**
 * Just made JosephMoniz' class serializable (http://blog.plasmaconduit.com/consistent-hashing/ and
 * https://github.com/JosephMoniz/scala-hash-ring and).
 * Here we can also experiment with the hash. E.g. MD5 instead of CRC32? Md5 is used in other
 * implementations because it has a good hash distribution)
 */
import java.util.zip.CRC32
import scala.collection.{SortedSet, Seq}
import scala.collection.mutable
import scala.collection.immutable.List

case class HashRingNode(value: String, weight: Int) extends java.io.Serializable

class SerializableHashRing(inputs: Seq[HashRingNode]) extends java.io.Serializable {

  private val _positions = inputs.foldLeft(List[(HashRingNode, List[Long])]()) { (memo, node) =>
    memo :+ (node, this._generatePositions(node))
  }

  private val _positionToNode = this._positions.foldLeft(mutable.Map[Long, String]()) { (memo, tuple) =>
    memo ++ tuple._2.foldLeft(mutable.Map[Long, String]()) { (memo, position) =>
      memo += (position -> tuple._1.value)
    }
  }

  private val _ring = this._positions.foldLeft(SortedSet[Long]()) { (memo, tuple) =>
    memo ++ tuple._2
  }

  def this(input: HashRingNode) = this(Seq(input))

  def get(value: String): Option[String] = {
    for {
      position <- this._hashToPosition(this._stringToCRC(value));
      node     <- this._positionToNode.get(position)
    } yield node
  }

  private def _hashToPosition(hash: Long): Option[Long]= {
    this._ring.from(hash).headOption orElse this._ring.headOption
  }

  private def _stringToCRC(input: String): Long = {
    val crc = new CRC32()
    crc.update(input.getBytes)
    crc.getValue
  }

  private def _generatePositions(node: HashRingNode): List[Long] = {
    val range = 1 to node.weight

    range.foldLeft(List[Long]()) { (memo, i) =>
      this._stringToCRC(node.value + i.toString)  :: memo
    }
  }

}