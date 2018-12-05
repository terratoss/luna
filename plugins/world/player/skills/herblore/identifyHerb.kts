import api.*
import io.luna.game.event.impl.ItemClickEvent.ItemFirstClickEvent
import io.luna.game.model.mob.Player
import world.player.skills.herblore.Herb

/**
 * Identifies [herb] for [player].
 */
fun identify(plr: Player, herb: Herb) {
    val herblore = plr.skill(SKILL_HERBLORE)
    val level = herb.level

    if (herblore.level >= level) {

        plr.inventory.remove(herb.idItem)
        plr.inventory.add(herb.identifiedItem)

        herblore.addExperience(herb.exp)

        val herbName = herb.identifiedItem.itemDef.name
        plr.sendMessage("You identify the $herbName.")
    } else {
        plr.sendMessage("You need a Herblore level of $level to identify this herb.")
    }
}

/**
 * Forwards to [identify] if [msg] contains a valid unidentified herb.
 */
fun tryIdentify(msg: ItemFirstClickEvent) {
    val herb = Herb.UNID_TO_HERB[msg.id]
    if (herb != null) {
        identify(msg.plr, herb)
        msg.terminate()
    }
}

/**
 * Listen for an unidentified herb clicks.
 */
on(ItemFirstClickEvent::class).run {
    val action = itemDef(it.id)?.inventoryActions?.get(0)
    if (action == "Identify") {
        tryIdentify(it)
    }
}