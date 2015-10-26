package cache.performance

import scala.util.{Failure, Success}
import scala.concurrent._
import scalaz.concurrent.Task
import scalaz._,Scalaz._

trait Scenarios extends Mixtures with Cache with Helper {

  println("Starting simulation...")

  import scala.concurrent.duration._
  import scalaz.stream._
  import time._

  implicit val sc = new java.util.concurrent.ScheduledThreadPoolExecutor(1)
  import scala.concurrent.ExecutionContext.Implicits.global

  def createAndGet(e: Event) = create(e).map(_.map(_.eventId).map(get).get)

  implicit class RichProcess[U](p: Process[Task, U]) {
    def start = p.run.timed(1000).runAsync(_ => ()) //start process asynchronously
  }

  implicit class ReportFuture[T](t: Future[T]) {
    def report(what: String) = t.asTask.runAsync(_.bimap(_.printStackTrace(), _ => println(what))) //print error if any
  }

  awakeEvery(1 second).map(_ => getEvents.take(1000).toList.map(createAndGet).threeTimesFlatten.report("NEW")).start

  awakeEvery(1 second).map(_ => getQueries.take(50).toList.map(query(_)).futureSequence.report("QUERY")).start

}

trait Helper { //flattening futures to extract failures

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit class FutureFlattener[T](f: List[Future[Future[T]]]) {
    def threeTimesFlatten = f.map(_.flatMap(identity)).futureSequence
  }

  implicit class FutureFlattener2[T](f: List[Future[T]]) {
    def futureSequence = Future.sequence(f)
  }

  implicit class FutureToTask[T](f: Future[T]) {
    def asTask = Task.async[T] {
      register =>
        f.onComplete {
          case Success(v) => register(v.right)
          case Failure(ex) => register(ex.left)
        }
    }
  }

  implicit val toFut = new Applicative[Future] {
    override def point[A](a: => A): Future[A] = Future.successful(a)

    override def ap[A, B](fa: => Future[A])(f: => Future[(A) => B]): Future[B] = f.flatMap(fa.map(_))
  }

}
