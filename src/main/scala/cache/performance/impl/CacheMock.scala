package cache.performance.impl
import cache.performance._

import scala.concurrent.Future

/**
 * Created by user on 10/24/15.
 */
trait CacheMock extends Cache {

  override def setupCache() = {}

  override def get(id: String): Future[Event] = Future.successful(Event(id, "", "", Map.empty[String, String]))

  override def update(eventId: String, propertyName: String, propertyValue: String): Future[Boolean] = Future.successful(true)

  override def bulkUpdate(messageId: String, propertyName: String, propertyValue: String): Future[Boolean] = Future.successful(true)

  override def subscribe(stmt: Pred, handler: (Event) => Unit): Unit = ()

  override def create(ev: Event): Future[Option[Event]] = Future.successful(Some(ev))

  override def query(stmt: Pred, page: Int, pageSize: Int): Future[Seq[Event]] =
    Future.successful(List(Event("", "", "", Map.empty[String, String])))
}


object CacheMockRunScenarios extends App with Scenarios with CacheMock with MeasuredCache