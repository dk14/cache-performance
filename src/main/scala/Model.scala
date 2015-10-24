/**
 * Created by user on 10/23/15.
 */
trait Model {

  case class Event(eventId: String, messageId: String, data: String, props: Map[String, String])

  sealed trait Pred
  case class Equal(lprop: String, rprop: String) extends Pred
  case class Like(lprop: String, rprop: String) extends Pred
  case class And(lpred: Pred, rpred: Pred) extends Pred
  case class Or(lpred: Pred, rpred: Pred) extends Pred


  implicit class RichPred(p: Pred) {
    def && (p2: Pred) = And(p, p2)
    def || (p2: Pred) = Or(p, p2)
  }

  implicit class RichString(s: String) {
    def === (s2: String) = Equal(s, s2)
    def ~~~ (s2: String) = Like(s, s2)
  }

}
