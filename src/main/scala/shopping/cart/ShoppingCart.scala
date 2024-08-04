package shopping.cart

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import akka.persistence.typed.scaladsl.{Effect, ReplyEffect}
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

  private def handleCommand(cartId: String, state: State, command: Command): ReplyEffect[Event, State] = {
    command match {
      case AddItem(itemId, quantity, replyTo) =>
        if (state.hasItem(itemId))
          Effect.reply(replyTo)(StatusReply.Error(s"Item '$itemId' was already added to this shopping cart"))
        else if (quantity <= 0)
          Effect.reply(replyTo)(StatusReply.Error("Quantity must be greater than zero"))
        else
          Effect
            .persist(ItemAdded(cartId, itemId, quantity))
            .thenReply(replyTo) { updatedCart => StatusReply.Success(Summary(updatedCart.items))}
    }
  }
}



