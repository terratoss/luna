package plugin

import java.util.Optional
import java.util.concurrent.ThreadLocalRandom

import io.luna.LunaContext
import io.luna.game.GameService
import io.luna.game.model.mobile.attr.AttributeValue
import io.luna.game.model.mobile.{MobileEntity, Npc, Player, PlayerRights}
import io.luna.game.model.{Position, World}
import io.luna.game.plugin.{PluginFailureException, PluginFunction, PluginManager}
import io.luna.game.task.Task
import io.luna.net.msg.out.{SendForceTabMessage, SendGameInfoMessage, SendWidgetTextMessage}

import scala.reflect.ClassTag

/** An object containing Scala code that interacts directly with Java code in order to allow a far more idiomatic approach to writing
  * plugins. All of the complex, high level, 'dirty work' is done in this file in order to ensure high usability of plugins.
  * <p>
  * '''Please ensure that great caution is taken when modifying the contents of this class.''' Scala code is heavily reliant on this
  * object and will not function if certain chunks of code are modified.
  *
  * @author lare96 <http://github.org/lare96>
  */
object ScalaBindings {

  // context instances, injected with the PluginBootstrap
  val ctx: LunaContext = null
  val plugins: PluginManager = null
  val world: World = null
  val service: GameService = null


  // type aliases
  type Mob = io.luna.game.model.mobile.MobileEntity
  type MobList[E <: Mob] = io.luna.game.model.mobile.MobileEntityList[E]

  type ItemDef = io.luna.game.model.`def`.ItemDefinition
  type NpcDef = io.luna.game.model.`def`.NpcDefinition


  // common constants, use `final val` to inline them
  final val rightsPlayer = PlayerRights.PLAYER
  final val rightsMod = PlayerRights.MODERATOR
  final val rightsAdmin = PlayerRights.ADMINISTRATOR
  final val rightsDev = PlayerRights.DEVELOPER


  // preconditions and plugin failure
  def fail(msg: => Any = "execution failure") = throw new PluginFailureException(msg)
  def failIf(cond: Boolean, msg: => Any = "cond == false") = if (cond) {fail(msg)}


  // aliases for utilities
  def rand = ThreadLocalRandom.current


  // message handling
  def on[T <: PluginEvent](func: (T, Player) => Unit)(implicit tag: ClassTag[T]) = plugins
    .submit(tag.runtimeClass, new PluginFunction(func))


  // implicit conversions
  implicit def asScalaOption[T](optional: Optional[T]): Option[T] = {
    if (optional.isPresent) {
      Some(optional.get)
    }
    else None
  }
  implicit def asJavaOptional[T](option: Option[T]): Optional[T] = {
    option match {
      case Some(it) => Optional.of(it)
      case None => Optional.empty()
    }
  }


  // enriched classes
  implicit class PlayerImplicits(player: Player) {
    def address = player.getSession.getHostAddress
    def sendMessage(message: String) = player.queue(new SendGameInfoMessage(message))
    def sendWidgetText(text: String, widget: Int) = player.queue(new SendWidgetTextMessage(text, widget))
    def sendForceTab(id: Int) = player.queue(new SendForceTabMessage(id))
  }

  implicit class MobileEntityImplicits(mob: MobileEntity) {
    def attr[T](key: String): T = {
      val attr: AttributeValue[T] = mob.getAttributes.get(key)
      attr.get
    }
    def attr[T](key: String, value: T) = {
      val attr: AttributeValue[T] = mob.getAttributes.get(key)
      attr.set(value)
    }
  }

  implicit class WorldImplicits(world: World) {
    def addNpc(id: Int, position: Position) = {
      val npc = new Npc(world.getContext, id, position)
      world.getNpcs.add(npc)
      npc
    }
    def schedule(instant: Boolean, delay: Int)(action: Task => Unit): Unit = {
      world.schedule(new Task(instant, delay) {
        override protected def execute() = action(this)
      })
    }
    def schedule(delay: Int)(action: Task => Unit): Unit = schedule(false, delay)(action)
  }

  implicit class ArrayImplicits[T](array: Array[T]) {
    def shuffle = {
      var i = array.length - 1
      while (i > 0) {
        val index = rand.nextInt(i + 1)
        val a = array(index)
        array(index) = array(i)
        array(i) = a
        i -= 1
        i + 1
      }
      array
    }
    def randElement = array((rand.nextDouble * array.length).toInt)
  }

  implicit class SeqImplicits[T](seq: Seq[T]) {
    def shuffle = util.Random.shuffle(seq)
    def randElement = seq((rand.nextDouble * seq.length).toInt)
  }
}