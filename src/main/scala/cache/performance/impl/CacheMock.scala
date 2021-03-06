package cache.performance.impl
import cache.performance._

import scala.concurrent.Future
import scalaz._, Scalaz._
/**
 * Created by user on 10/24/15.
 */

import Model._

trait CacheMock extends Cache {

  def name: String = "mock"

  override def setupCache() = {}

  override def get(id: String): Future[Option[Event]] = Event(id, "", "", Map.empty[String, String]).some.point[Future] //same as Future(Event(...)) but cleaner, it workds because of Helper.toFut implicit

  override def update(eventId: String, propertyName: String, propertyValue: String): Future[Unit] = ().point[Future] //looks better than Future.successful(()), isn't it?

  override def bulkUpdate(pred: Pred, propertyName: String, propertyValue: String): Future[Unit] = ().point[Future]

  override def subscribe(stmt: Pred, handler: (Event, Event) => Unit): Unit = ()

  override def create(ev: Event): Future[Event] = ev.point[Future]

  override def query(stmt: Pred, page: Int, pageSize: Int): Future[Seq[Event]] =
    List(Event("", "", "", Map.empty[String, String])).point[Future]
}


object CacheMockRunScenarios extends App with Scenarios with CacheMock with MeasuredCache