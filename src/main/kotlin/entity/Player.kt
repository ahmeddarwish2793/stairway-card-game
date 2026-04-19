package entity

/**
 * Represents a player participating in the Kartentreppe game.
 *
 * A player has a [name], a current [score], a mutable hand of cards [handCards],
 * and a pile of [collectedCards] gained during play. The flag [hasDestroyed] can be
 * used by the game logic to remember whether the player destroyed a
 * staircase card during his turn once.
 *
 * The lists [handCards] and [collectedCards] are created empty by default; the
 * game service is expected to deal initial cards to the player.
 *
 * @constructor Creates a player with a [name] and initial [score],
 * [handCards], [collectedCards], and [hasDestroyed] state.
 * @property name Display name of the player (shown in UI/logs).
 * @property score Current score of the player (defaults to 0).
 * @property handCards The cards currently held in the player's hand.
 * @property collectedCards Cards the player has collected (from valid combines).
 * @property hasDestroyed Whether the player has destroyed a staircase card in the current round.
 */

class Player(val name: String) {
    var score: Int = 0
    val handCards: MutableList<Card> = mutableListOf()
    val collectedCards: MutableList<Card> = mutableListOf()
    var hasDestroyed : Boolean = false


    override fun toString(): String = "$name(score = $score)"
}

