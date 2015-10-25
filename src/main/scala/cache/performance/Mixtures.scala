package cache.performance

/**
 * Created by user on 10/24/15.
 */
trait Mixtures extends Model {

  import scala.util.Random
  private implicit class RandomizedList[T](l: List[T]) {
    def random = Iterator.continually(Random.shuffle(l).toIterator).flatten
  }

  def getEvents: Iterator[Event] = List(Event("0", "0", "data", Map("a" -> "1"))).random

  def getQueries: Iterator[Pred] = List("a" === "1" && "b" === "3").random

}
