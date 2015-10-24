/**
 * Created by user on 10/23/15.
 */

import scala.concurrent._
trait Cache extends Model {

  def get(id: String): Future[Event]
  def query(stmt: Pred, page: Int = 1, pageSize: Int = 20): Future[Seq[Event]]
  def create(ev: Event): Future[Option[Event]]
  def update(eventId: String, propertyName: String, propertyValue: String): Future[Boolean]
  def bulkUpdate(messageId: String, propertyName: String, propertyValue: String): Future[Boolean]
  def subscribe(stmt: Pred, handler: Event => Unit): Unit

}


import nl.grons.metrics.scala._

trait MeasuredCache extends Cache with Instrumented {

  abstract override def get(id: String): Future[Event] = measure("read")(super.get(id))

  abstract override def query(stmt: Pred, page: Int = 1, pageSize: Int = 20): Future[Seq[Event]] =
    measure("query")(super.query(stmt, page, pageSize))

  abstract override def create(ev: Event): Future[Option[Event]] = measure("create")(super.create(ev))

  abstract override def update(eventId: String, propertyName: String, propertyValue: String): Future[Boolean] =
    measure("update")(super.update(eventId, propertyName, propertyValue))

  abstract override def bulkUpdate(messageId: String, propertyName: String, propertyValue: String): Future[Boolean] =
    measure("bulkUpdate")(super.bulkUpdate(messageId, propertyName, propertyValue))

  abstract override def subscribe(stmt: Pred, handler: Event => Unit): Unit = super.subscribe(stmt, e => {
    metrics.counter("trigger.count").inc()
    handler(e)
  })

}


object Application {
  val metricRegistry = new com.codahale.metrics.MetricRegistry()
}

trait Instrumented extends nl.grons.metrics.scala.InstrumentedBuilder {
  val metricRegistry = Application.metricRegistry

  def measure[T](name: String)(f: => Future[T]): Future[T] = {
    val ctx = metrics.timer(name).timerContext()
    metrics.counter(name + ".count").inc()
    f.map{ x => ctx.stop(); x }
  }

}