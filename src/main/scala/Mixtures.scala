/**
 * Created by user on 10/24/15.
 */
trait Mixtures extends Model {

  def getEvents: Iterator[Event] = List(Event("0", "0", "data", Map("a" -> "1"))).toIterator

  def getQueries: Iterator[Pred] = List("a" === "1" && "b" === "3").toIterator

}
