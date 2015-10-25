package cache.performance.impl
import cache.performance._
import com.datastax.driver.core._
import com.datastax.spark.connector._
import com.datastax.spark.connector.rdd.CassandraTableScanRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.concurrent._


trait CassandraCache extends Cache {

  def name: String

  val cluster = Cluster.builder().build()

  val session = cluster.connect("space")

  import com.google.common.util.concurrent._

  implicit class ToFutureList(f: ResultSetFuture){
    import scala.collection.JavaConverters._

    val p = Promise[ResultSet]()
    val callback = new FutureCallback[ResultSet] {
      override def onFailure(t: Throwable): Unit = p.failure(t)
      override def onSuccess(result: ResultSet): Unit = p.success(result)
    }

    def asScala = {
      Futures.addCallback(f, callback)
      p.future.map(_.all().asScala)
    }

  }

  implicit class ToSolrPredicate(p: Pred) {
    def asSolr: String = p match {
      case And(p1, p2) => s"(${p1.asSolr} AND ${p2.asSolr})" //TODO transform names for props Map correctly
      case Or(p1, p2) => s"(${p1.asSolr} OR ${p2.asSolr})"
      case Like(p1, p2) => s"$p1:*$p2*"
      case Equal(p1, p2) => s"$p1:$p2"
    }
  }

  implicit class ToSparkPredicate[T](p: CassandraTableScanRDD[T]) {
    def withPredicate(pred: Pred): RDD[T] = p.select("*").where(s"solr='${pred.asSolr}'")
  }


  def setupCache(): Unit

  def get(id: String): Future[Event] =
    session.executeAsync("SELECT * FROM events WHERE eventId=?0", id).asScala.map(_.map(rowToEvent).head)

  def query(stmt: Pred, page: Int = 1, pageSize: Int = 20): Future[Seq[Event]] =
    session.executeAsync("SELECT * FROM events WHERE solr='?0'", stmt.asSolr).asScala.map(_.map(rowToEvent))

  def create(ev: Event): Future[Option[Event]] = {
    import ev._
    val mkProps = props.map{case (k,v) => s"'$k':'$v'"}.mkString(",")
    val res = session.executeAsync(s"INSERT INTO events VALUES( '$eventId','$messageId','$data',{$props} )")
    res.asScala.map(_ => Some(ev))
  }

  def update(eventId: String, propertyName: String, propertyValue: String): Future[Unit] =
    session.executeAsync(s"UPDATE events SET props['$propertyName']='$propertyValue' WHERE eventId='$eventId'").asScala.map(_ => ())

  def bulkUpdate(stmt: Pred, propertyName: String, propertyValue: String): Future[Unit] =
    session.executeAsync(s"UPDATE events SET props['$propertyName']='$propertyValue' WHERE solr='${stmt.asSolr}'").asScala.map(_ => ())

  import com.datastax.spark.connector.streaming._
  import org.apache.spark._
  def sparkConf: SparkConf
  val ssc = new StreamingContext(sparkConf, Seconds(1))
  val rdd = ssc.cassandraTable("space", name)

  def subscribe(stmt: Pred, handler: (Event, Event) => Unit): Unit =
    rdd.withPredicate(stmt).map(cassandraRowToEvent).map(x => x -> x).foreach(handler.tupled) //gonna be executed in Spark cluster

  private def rowToEvent(r: Row) = cassandraRowToEvent(CassandraRow.fromJavaDriverRow(r, Array("eventId", "messageId", "data", "props")))

  private def cassandraRowToEvent(r: CassandraRow) = Event(r.get("eventId"), r.get("messageId"), r.get("data"), r.get("props"))


}



