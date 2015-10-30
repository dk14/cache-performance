package cache.performance.impl

/**
 * Created by user on 10/30/15.
 */

import java.util.concurrent.Executors

import cache.performance._
import Model._
import com.tangosol.io.pof.reflect.{PofValue, PofNavigator, SimplePofPath}
import com.tangosol.io.pof.{PofReader, PofWriter, PortableObject}
import com.tangosol.net.CacheFactory
import com.tangosol.net.NamedCache
import com.tangosol.util.Filter
import com.tangosol.util.extractor.PofExtractor
import com.tangosol.util.filter.{EqualsFilter, LikeFilter, OrFilter, AndFilter}

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._


trait CoherenceCache extends Cache {
  def name: String = "dcoh"

  import Extractors._

  lazy val cache = {
    CacheFactory.ensureCluster()
    CacheFactory.getCache("events")
  }

  implicit class ToPof(ev: Event)  {
    def pof = new PofEvent(ev)
  }

  implicit class ToHazelPredicate(p: Pred) {
    def asFilter: Filter = p match {
      case And(p1, p2) => new AndFilter(p1.asFilter, p2.asFilter)
      case Or(p1, p2) => new OrFilter(p1.asFilter, p2.asFilter)
      case Like(p1, p2) => new LikeFilter(customPropExtractor(p1), p2, '\u0000', false)
      case Equal(p1, p2) => new EqualsFilter(customPropExtractor(p1), p2)
    }
  }

  private val pool = Executors.newFixedThreadPool(20) //because it doesn't support async operations

  private implicit val es = ExecutionContext.fromExecutorService(pool)

  def setupCache(): Unit = {}

  def get(id: String): Future[Option[Event]] = Future {
    Option(cache.get(id).asInstanceOf[PofEvent].get)
  }

  def query(stmt: Pred, page: Int = 1, pageSize: Int = 20): Future[Seq[Event]] = Future {
    cache.entrySet(stmt.asFilter).asScala.toList.asInstanceOf[List[PofEvent]].map(_.get)
  }

  def create(ev: Event): Future[Event] = Future {
    cache.put(ev.eventId, ev.pof)
    ev
  }

  def update(eventId: String, propertyName: String, propertyValue: String): Future[Unit] = ???

  def bulkUpdate(stmt: Pred, propertyName: String, propertyValue: String): Future[Unit] = ???

  def subscribe(stmt: Pred, handler: (Event, Event) => Unit): Unit = ???


}

object Extractors {
  import Portability._

  def getProps(s: String) = s split "," map { x =>
    val split = x.split("=")
    split(0) -> split(1)
  } toMap

  def customPropExtractor(name: String) = new PofExtractor(classOf[String], new SimplePofPath(knownFieldsList.indexOf(name)))
}

class PofEvent(input: Event) extends Serializable with PortableObject {

  //http://docs.oracle.com/cd/E24290_01/coh.371/e23131/usepof.htm#CHDJDFDF

  def this() = this(null)

  import Portability._
  import Extractors._

  private var e: Event = input

  def get: Event = e

  override def readExternal(pofReader: PofReader): Unit = {
    import pofReader._
    val eventId = readString(0)
    val messageId = readString(1)
    val data = readString(2)
    val unknownProps = getProps(readString(3))

    val props = knownFieldsList.zipWithIndex.map(n => n._1 -> readString(n._2 + 4)).toMap ++ unknownProps

    e = Event(eventId, messageId, data, props)
  }

  override def writeExternal(pofWriter: PofWriter): Unit = {
    import pofWriter._
    writeString(0, e.eventId)
    writeString(1, e.messageId)
    writeString(2, e.data)
    val etc = e.props.filter(x => !knownFields.contains(x._1))
    val mk = (a: String, b: String) => a + "=" + b
    writeString(3, etc.map(mk.tupled).reduce(_ + "," + _))
    for ((f, i) <- knownFieldsList.zipWithIndex; v <- e.props.get(f)) writeString(i + 4, v)
  }
}

object CoherenceCacheScenarios extends App with CoherenceCache with MeasuredCache with Scenarios
