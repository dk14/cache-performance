package cache.performance

/**
 * Created by user on 10/23/15.
 */

import java.util.concurrent.TimeUnit
import com.codahale.metrics.ConsoleReporter
import nl.grons.metrics.scala.MetricName
import statsd.{StatsdReporter, Statsd}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

trait Cache extends Model {

  def setupCache(): Unit
  def get(id: String): Future[Event]
  def query(stmt: Pred, page: Int = 1, pageSize: Int = 20): Future[Seq[Event]]
  def create(ev: Event): Future[Option[Event]]
  def update(eventId: String, propertyName: String, propertyValue: String): Future[Unit]
  def bulkUpdate(stmt: Pred, propertyName: String, propertyValue: String): Future[Unit]
  def subscribe(stmt: Pred, handler: Event => Unit): Unit

}

trait MeasuredCache extends Cache with Instrumented {

  abstract override def get(id: String): Future[Event] = measure("read")(super.get(id))

  abstract override def query(stmt: Pred, page: Int = 1, pageSize: Int = 20): Future[Seq[Event]] =
    measure("query")(super.query(stmt, page, pageSize))

  abstract override def create(ev: Event): Future[Option[Event]] = measure("create")(super.create(ev))

  abstract override def update(eventId: String, propertyName: String, propertyValue: String): Future[Unit] =
    measure("update")(super.update(eventId, propertyName, propertyValue))

  abstract override def bulkUpdate(stmt: Pred, propertyName: String, propertyValue: String): Future[Unit] =
    measure("bulkUpdate")(super.bulkUpdate(stmt, propertyName, propertyValue))

  abstract override def subscribe(stmt: Pred, handler: Event => Unit): Unit = super.subscribe(stmt, e => {
    metrics.counter("trigger.count").inc()
    handler(e)
  })

}


object Application {
  val metricRegistry = new com.codahale.metrics.MetricRegistry()

  val statsd = new Statsd("192.168.99.100", 8125)

  val remoteReporter = StatsdReporter.forRegistry(metricRegistry)
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .convertRatesTo(TimeUnit.SECONDS)
    .build(statsd)


  val localReporter = ConsoleReporter.forRegistry(metricRegistry).build()

  //localReporter.start(15, TimeUnit.SECONDS)

  remoteReporter.start(1, TimeUnit.SECONDS)

}

trait Instrumented extends nl.grons.metrics.scala.InstrumentedBuilder {

  override lazy val metricBaseName = MetricName("cache-performance")

  lazy val metricRegistry = Application.metricRegistry

  def measure[T](name: String)(f: => Future[T]): Future[T] = {
    val ctx = metrics.timer(name).timerContext()
    metrics.counter(name + ".count").inc()
    f.map{ x => ctx.stop(); x }
  }

}