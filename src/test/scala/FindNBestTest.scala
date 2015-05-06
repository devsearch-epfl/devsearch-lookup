import devsearch.lookup.FindNBestNew
import org.scalatest._

class FindNBestTest extends FlatSpec with Matchers {

  def int2float(arg: Int): Float = arg

  "FindNBest" should "return the requested number of matches" in {
    val input = Stream(1,2,3,4,5)
    FindNBestNew(input, int2float, 3).length shouldEqual 3
  }

  it should "work if we request too many results" in {
    val input = Stream(1,2,3)
    FindNBestNew(input, int2float, 100).length shouldEqual 3
  }

  it should "return only best results" in {
    val input = Stream(1,2,3,4,5)
    FindNBestNew(input, int2float, 1) shouldEqual List(5)
  }

  it should "work correctly when multiple items have same score" in {
    val input = Stream(('a',0), ('b',0), ('c',1), ('d',1))
    val result = FindNBestNew[(Char,Int)](input, _._2, 3)
    result should contain ('c',1)
    result should contain ('d',1)
  }

  it should "return results sorted" in {
    val input = Stream(3,5,1,2,4)
    FindNBestNew(input, int2float, 5) shouldEqual List(5,4,3,2,1)
  }
}
