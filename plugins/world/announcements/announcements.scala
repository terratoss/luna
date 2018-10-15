import io.luna.game.event.impl.ServerLaunchEvent
import io.luna.game.model.mob.Player

/* Announcement broadcast interval. */
private val TICK_INTERVAL = 1500 // 15 mins

/* Messages that will be randomly announced. */
private val MESSAGES = Vector(
  "Luna is a Runescape private server for the #317 protocol.",
  "Luna can be found on GitHub under luna-rs/luna",
  "Change these messages in /plugins/world/announcements/announcements.scala",
  "Any bugs found using Luna should be reported to the GitHub page."
)

/* A filter for players that will receive the announcement. */
private val FILTER = (plr: Player) => plr.rights <= RIGHTS_ADMIN // Only players below admin rank.


/* Filter players and broadcast announcements at set intervals. */
on[ServerLaunchEvent] { msg =>
  world.scheduleForever(TICK_INTERVAL) {
    world.players.
      filter(FILTER).
      foreach { _.sendMessage(pick(MESSAGES)) }
  }
}