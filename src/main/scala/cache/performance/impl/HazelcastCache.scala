package cache.performance.impl

import java.util.Map.Entry
import java.util.concurrent.{Executors}

import cache.performance._
import com.hazelcast.cache.ICache
import com.hazelcast.core._
import com.hazelcast.config._
import javax.cache._

import com.hazelcast.map.listener.EntryUpdatedListener
import com.hazelcast.map.{EntryBackupProcessor, EntryProcessor}
import com.hazelcast.nio.serialization.{PortableFactory, PortableReader, PortableWriter, Portable}

import scala.concurrent._

trait HazelcastCache extends cache.performance.Cache with Portability {

  import scala.collection.JavaConverters._

  def name: String = "hazel"

  def config: Config

  val manager = Caching.getCachingProvider().getCacheManager()

  def instance: HazelcastInstance

  private val cache = manager.getCache[String, Event](name).asInstanceOf[ICache[String, PortableEvent]]

  private val map = instance.getMap[String, PortableEvent](name)

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

  def create(ev: Event): Future[Option[Event]] = cache.putAsync(ev.eventId, ev).asScala.map(_ => Some(ev.get))

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

    val listener = new EntryUpdatedListener[String, Event] {
      override def entryUpdated(event: EntryEvent[String, Event]): Unit = handler(event.getOldValue, event.getValue)
    }

  }

}

trait Portability extends Model {

  val ClassId = 100500

  val FactoryId = 1

  def addPortability(cfg: Config) = cfg.getSerializationConfig().addPortableFactory(FactoryId, new PortableFactory {
    def create(classId: Int ) = if ( ClassId == classId ) new PortableEvent(null) else null
  })

  implicit class PortableEvent(input: Event) extends Portable {

    private var e: Event = input
    def get: Event = e

    override def readPortable(reader: PortableReader): Unit = {
      def readString(name: String) = reader.readCharArray(name).mkString

      val eventId = readString("eventId")
      val messageId = readString("messageId")
      val data = readString("data")
      val propNames = readString("propNames").split(",")

      val props = propNames.map(n => n -> readString(n)).toMap

      e = Event(eventId, messageId, data, props)
    }

    override def writePortable(writer: PortableWriter): Unit = {
      def writeString(name: String, value: String) = writer.writeCharArray(name, value.toArray)
      writeString("eventId", e.eventId)
      writeString("messageId", e.messageId)
      writeString("data", e.data)
      writeString("propNames", e.props.keys.mkString(","))
      e.props.foreach((writeString _).tupled)
    }

    override def getFactoryId: Int = FactoryId

    override def getClassId: Int = ClassId
  }

}

object HazelCastCacheScenarios extends App with HazelcastCache with MeasuredCache with Scenarios {

  override lazy val config: Config = {
    val cfg = new Config()
    cfg.getNetworkConfig.getJoin.getMulticastConfig.setEnabled(false)
    cfg.getNetworkConfig.getJoin.getTcpIpConfig.setEnabled(true)
    cfg.getNetworkConfig.getJoin.getTcpIpConfig.addMember("hazelseed")
    addPortability(cfg)
    cfg
  }

  override lazy val instance: HazelcastInstance = Hazelcast.newHazelcastInstance(config)

}