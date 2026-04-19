package entity

typealias Column = ArrayDeque<Card>


/**
 * Holds the complete, mutable state of a Kartentreppe match.
 *
 * The game board is modeled as a list of staircase columns ([staircase]), where each
 * column is a stack ([ArrayDeque]) of [Card]s.
 *
 * The draw pile is [drawStack] ; the discard pile is [discardStack].
 * The textual [log] records game events for debugging or GUI display.
 *
 * The game is played by exactly two players, [player1] and [player2]. The pointer
 * [currentPlayer] identifies whose turn it is at any time.
 *
 * The flag [hasRemoved] tracks whether any player has removed a staircase card since
 * the last shuffle (used by end-of-game rule 3).
 *
 * - A card may exist in exactly one container at a time (hand, staircase column, draw, or discard).
 * - [currentPlayer] is either [player1] or [player2].
 * - [discardStack] and [log] are empty at game start.
 *
 * @property staircase Read-only list of staircase columns; each column is a stack of cards.
 * @property drawStack Face-down stack used to draw new cards.
 * @property discardStack Face-up stack that receives played/discarded cards; starts empty.
 * @property log Append-only message list for game events; starts empty.
 * @property player1 First player.
 * @property player2 Second player.
 * @property currentPlayer The player whose turn it is (either [player1] or [player2]).
 * @property hasRemoved Whether a staircase card has been removed since the last shuffle (end-game rule helper).
 */


class KartentreppeGame(val player1: Player, val player2: Player) {
    val staircase: List<Column> = List(5) { ArrayDeque<Card>() }
    val drawStack: ArrayDeque<Card> = ArrayDeque()
    val discardStack: ArrayDeque<Card> = ArrayDeque()
    val log: MutableList<String> = mutableListOf()

    var currentPlayer: Player = player1
    var hasRemoved: Boolean = false


    override fun toString(): String =
        "KartentreppeGame(currentPlayer=${currentPlayer.name}, draw=${drawStack.size}, discard=${discardStack.size})"
}
