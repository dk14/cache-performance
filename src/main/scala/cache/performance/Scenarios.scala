package cache.performance

import scalaz.concurrent.Task

trait Scenarios extends Mixtures with Cache {

  println("Starting simulation...")

  import scala.concurrent.ExecutionContext.Implicits.global

  import scala.concurrent.duration._
  import scalaz.stream._
  import time._

  implicit val sc = new java.util.concurrent.ScheduledThreadPoolExecutor(1)
  import scala.concurrent.ExecutionContext.Implicits.global

  def createAndGet(e: Event) = create(e).map(_.map(_.eventId).map(get))

  Task.fork(awakeEvery(1 second).map(_ => getEvents.take(1000).toList.map(createAndGet)).run).runAsync(_ => ())

  Task.fork(awakeEvery(1 second).map(_ => getQueries.take(50).toList.map(query(_))).run).runAsync(_ => ())

}
