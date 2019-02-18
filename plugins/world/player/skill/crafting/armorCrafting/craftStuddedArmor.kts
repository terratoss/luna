import api.predef.*
import io.luna.game.action.Action
import io.luna.game.action.ProducingAction
import io.luna.game.model.item.Item
import io.luna.game.model.mob.Player
import world.player.skill.crafting.armorCrafting.HideArmor

/**
 * A [ProducingAction] implementation that will make studded armor.
 */
class MakeStuddedAction(val plr: Player, val armor: HideArmor, val removeId: Int) :
        ProducingAction(plr, true, 2, Int.MAX_VALUE) {

    companion object {

        /**
         * The steel studs identifier.
         */
        val STUDS = 2370
    }

    override fun canProduce(): Boolean =
        when {
            plr.crafting.level < armor.level -> {
                plr.sendMessage("You need a Crafting level of ${armor.level} to make this.")
                false
            }
            else -> true
        }

    override fun onProduce() {
        plr.crafting.addExperience(armor.exp)
    }

    override fun remove(): Array<Item> = arrayOf(Item(removeId), Item(STUDS))

    override fun add(): Array<Item> = arrayOf(Item(armor.id))

    override fun isEqual(other: Action<*>?): Boolean =
        when (other) {
            is MakeStuddedAction -> armor == other.armor
            else -> false
        }
}

/**
 * The steel studs identifier.
 */
val steelStuds = MakeStuddedAction.STUDS

/**
 * The leather body identifier.
 */
val leatherBody = HideArmor.LEATHER_BODY.id

/**
 * The leather chaps identifier.
 */
val leatherChaps = HideArmor.LEATHER_CHAPS.id

/**
 * Craft studded [armor] for [plr].
 */
fun makeStudded(plr: Player, armor: HideArmor) {
    when (armor) {
        HideArmor.STUDDED_BODY ->
            plr.submitAction(MakeStuddedAction(plr, armor, leatherBody))
        HideArmor.STUDDED_CHAPS ->
            plr.submitAction(MakeStuddedAction(plr, armor, leatherChaps))
    }
}

// Make studded body.
useItem(steelStuds)
    .onItem(leatherBody) {
        makeStudded(plr, HideArmor.STUDDED_BODY)
    }

// Make studded chaps.
useItem(steelStuds)
    .onItem(leatherChaps) {
        makeStudded(plr, HideArmor.STUDDED_CHAPS)
    }