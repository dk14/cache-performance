package cache.performance.impl

import java.util.Map.Entry
import java.util.concurrent.{Executors}
import javax.cache.configuration.MutableConfiguration

import cache.performance._
import com.hazelcast.cache.ICache
import com.hazelcast.core._
import com.hazelcast.config._
import javax.cache._

import com.hazelcast.map.listener.EntryUpdatedListener
import com.hazelcast.map.{EntryBackupProcessor, EntryProcessor}
import com.hazelcast.nio.serialization._

import scala.concurrent._
import Model._

trait HazelcastCache extends cache.performance.Cache {

  import scala.collection.JavaConverters._

  import Portability._
  implicit def toPortable(ev: Event) = new PortableEvent(ev)

  def name: String = "hazel"

  def config: Config

  lazy val manager = Caching.getCachingProvider().getCacheManager()

  def instance: HazelcastInstance

  val configuration = new MutableConfiguration[String, PortableEvent]()

  private lazy val cache = manager.createCache[String, PortableEvent, MutableConfiguration[String, PortableEvent]](name, configuration).unwrap( classOf[ICache[String, Event]])

  private lazy val map = instance.getMap[String, PortableEvent](name)

  private val fixedQueryPool = Executors.newFixedThreadPool(20) //because it doesn't support async queries

  private implicit val es = ExecutionContext.fromExecutorService(fixedQueryPool)

  override def setupCache(): Unit = {}

  implicit class ToScalaFuture[T](f: ICompletableFuture[T]) {
    def asScala = {

      val p = Promise[T]()
      f.andThen(new ExecutionCallback[T] {
        override def onFailure(t: Throwable): Unit = p.failure(t)

        override def onResponse(response: T): Unit = p.success(response)
      })
      p.future
    }
  }

  import com.hazelcast.query._

  implicit class ToHazelPredicate(p: Pred) {
    def asHazel: Predicate[_, _] = p match {
      case And(p1, p2) => Predicates.and(p1.asHazel, p2.asHazel)
      case Or(p1, p2) => Predicates.or(p1.asHazel, p2.asHazel)
      case Like(p1, p2) => Predicates.like(p1, p2)
      case Equal(p1, p2) => Predicates.equal(p1, p2)
    }
  }


  def get(id: String): Future[Event] = cache.getAsync(id).asScala.map(_.get)

  def query(stmt: Pred, page: Int = 1, pageSize: Int = 20): Future[Seq[Event]] = Future {
    map.values(stmt.asHazel).asScala.toSeq.view.map(_.get)
  }

  import scalaz._, Scalaz._

  def create(ev: Event): Future[Option[Event]] = //execute two operations and merge futures
    (Future(map.put(ev.eventId, ev)) |@| cache.putAsync(ev.eventId, ev).asScala).tupled.map(_ => Some(ev.get))

  def update(eventId: String, propertyName: String, propertyValue: String): Future[Unit] = {
    val processor = new EntryProcessor[String, PortableEvent] {
      override def process(entry: Entry[String, PortableEvent]): AnyRef = ???

      override def getBackupProcessor: EntryBackupProcessor[String, PortableEvent] = ???
    }
    Future {
      map.executeOnEntries(processor, ("eventId" === eventId).asHazel)
    }
  }

  def bulkUpdate(stmt: Pred, propertyName: String, propertyValue: String): Future[Unit] = {
    val processor = new EntryProcessor[String, PortableEvent] {
      override def process(entry: Entry[String, PortableEvent]): AnyRef = ???

      override def getBackupProcessor: EntryBackupProcessor[String, PortableEvent] = ???
    }
    Future {
      map.executeOnEntries(processor, stmt.asHazel)
    }
  }

  def subscribe(stmt: Pred, handler: (Event, Event) => Unit): Unit = {

    val listener = new EntryUpdatedListener[String, PortableEvent] {
      override def entryUpdated(event: EntryEvent[String, PortableEvent]): Unit = handler(event.getOldValue.get, event.getValue.get)
    }

  }

}

object Portability {

  val ClassId = 100500

  val FactoryId = 1

  def addPortability(cfg: Config) = cfg.getSerializationConfig().addPortableFactory(FactoryId, new PortableFactory {
    def create(classId: Int ) = if ( ClassId == classId ) new PortableEvent(null) else null
  })



}

class PortableEvent(input: Event) extends Portable {
  import Portability._
  private var e: Event = input
  def get: Event = e

  override def readPortable(reader: PortableReader): Unit = {
    def readString(name: String) = reader.readUTF(name)

    //println("readPortable!!!!!")

    val eventId = readString("eventId")
    val messageId = readString("messageId")
    val data = readString("data")
    val propNames = readString("propNames").split(",")

    val props = propNames.map(n => n -> readString(n)).toMap

    e = Event(eventId, messageId, data, props)
  }

  override def writePortable(writer: PortableWriter): Unit = {

    //println("writePortable!!!!")

    def writeString(name: String, value: String) = writer.writeUTF(name, value)
    writeString("eventId", e.eventId)
    writeString("messageId", e.messageId)
    writeString("data", e.data)
    writeString("propNames", e.props.keys.mkString(","))
    e.props.foreach((writeString _).tupled)
  }

  override def getFactoryId: Int = FactoryId

  override def getClassId: Int = ClassId
}

object HazelCastCacheScenarios extends App with HazelcastCache with MeasuredCache with Scenarios {

  import Portability._
  override lazy val config: Config = {
    val cfg = new Config()
    cfg.getNetworkConfig.getJoin.getMulticastConfig.setEnabled(false)
    cfg.getNetworkConfig.getJoin.getTcpIpConfig.setEnabled(true)
    cfg.getNetworkConfig.getJoin.getTcpIpConfig.addMember("hazelseed")

    //val classDefinition = new ClassDefinitionBuilder(FactoryId, ClassId)
    //  .addUTFField("a").addUTFField("b").addUTFField("propNames").addUTFField("eventId").addUTFField("messageId").addUTFField("data").build()
    //cfg.getSerializationConfig().addClassDefinition(classDefinition)

    addPortability(cfg)
    cfg
  }

  override lazy val instance: HazelcastInstance = Hazelcast.newHazelcastInstance(config)

}