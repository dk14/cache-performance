package cache.performance

import Model._
trait Mixtures {

  import scala.util.Random
  private implicit class RandomizedList[T](l: List[T]) {
    def random = Iterator.continually(Random.shuffle(l).toIterator).flatten
  }

  lazy val data = List.fill(10000)("data" + Random.nextInt()).mkString
  lazy val bonus = List.fill(10)("field" + Math.abs(Random.nextInt()) -> "value").toMap

  def getEvents: Iterator[Event] = List(
    Event(Random.nextInt().toString, "0", data, Map("a" -> "1") ++ bonus),
    Event(Random.nextInt().toString, "0", data, Map("a" -> "1", "b" -> "3") ++ bonus),
    Event(Random.nextInt().toString, "0", data, Map("a" -> "1", "b" -> "4") ++ bonus),
    Event(Random.nextInt().toString, "0", data, Map("a" -> "11", "b" -> "4") ++ bonus),
    Event(Random.nextInt().toString, "0", data, Map("a" -> "11", "b" -> "4", "c" -> "6") ++ bonus)
  ).random

  def getQueries: Iterator[Pred] = List(
    "a" === "1" && "b" === "3",
    "a" === "1" && ("b" === "4" || "b" === "3"),
    "a" ~~~ "1",
    "a" ~~~ Random.nextInt().toString

  ).random

}
