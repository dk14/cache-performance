package cache.performance.impl
import cache.performance._

import scala.concurrent.Future
import scalaz._, Scalaz._
/**
 * Created by user on 10/24/15.
 */
trait CacheMock extends Cache {



  def name: String = "mock"

  override def setupCache() = {}

  override def get(id: String): Future[Event] = Event(id, "", "", Map.empty[String, String]).point[Future]

  override def update(eventId: String, propertyName: String, propertyValue: String): Future[Unit] = ().point[Future]

  override def bulkUpdate(pred: Pred, propertyName: String, propertyValue: String): Future[Unit] = ().point[Future]

  override def subscribe(stmt: Pred, handler: (Event, Event) => Unit): Unit = ()

  override def create(ev: Event): Future[Option[Event]] = ev.some.point[Future]

  override def query(stmt: Pred, page: Int, pageSize: Int): Future[Seq[Event]] =
    List(Event("", "", "", Map.empty[String, String])).point[Future]
}


object CacheMockRunScenarios extends App with Scenarios with CacheMock with MeasuredCache