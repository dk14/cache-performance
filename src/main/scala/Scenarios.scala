import scalaz.concurrent.Task

trait Scenarios extends Mixtures with MeasuredCache {

  import scalaz.stream._
  import time._
  import scala.concurrent.duration._

  implicit val sc = new java.util.concurrent.ScheduledThreadPoolExecutor(1)
  import scala.concurrent.ExecutionContext.Implicits.global

  def createAndGet(e: Event) = create(e).map(_.map(_.eventId).map(get))

  Task.fork(awakeEvery(1 second).take(300).map(_ => getEvents.take(1000).map(createAndGet)).run).run

  Task.fork(awakeEvery(1 second).take(300).map(_ => getQueries.take(50).map(query(_))).run).run

}
