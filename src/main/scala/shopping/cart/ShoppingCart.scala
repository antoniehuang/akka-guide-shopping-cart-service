package shopping.cart

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import akka.serialization.jackson.CborSerializable

object ShoppingCart {

  // The current state held by the `EventSourcedBehavior.
  sealed trait Command extends CborSerializable

  final case class AddItem(
                            itemId: String,
                            quantity: Int,
                            replyTo: ActorRef[StatusReply[Summary]]) extends Command

  final case class Summary(items: Map[String, Int]) extends CborSerializable

  sealed trait Event extends CborSerializable {
    def cartId: String
  }


  final case class ItemAdded(cartId: String, itemId: String, quantity: Int) extends Event

  final case class State(items: Map[String, Int]) extends CborSerializable {
    def hasItem(itemId: String): Boolean =
      items.contains(itemId)

    def isEmpty: Boolean =
      items.isEmpty

    def updateItem(itemId: String, quantity: Int): State = {
      quantity match {
        case 0 => copy(items = items - itemId)
        case _ => copy(items = items + (itemId -> quantity))
      }
    }
  }
  object State {
    val empty =
      State(items = Map.empty)
  }
}



