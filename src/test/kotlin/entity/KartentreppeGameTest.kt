package entity

import kotlin.test.*

/**
 * Tests for [KartentreppeGame].
 *
 * The tests verify correct initialization, behavior of stacks, player switching,
 * and consistent game state changes during typical actions.
 */
class KartentreppeGameTest {

    private lateinit var p1: Player
    private lateinit var p2: Player

    private lateinit var c1: ArrayDeque<Card>
    private lateinit var c2: ArrayDeque<Card>
    private lateinit var draw: ArrayDeque<Card>
    private lateinit var discard: ArrayDeque<Card>

    private lateinit var game: KartentreppeGame

    /**
     * sets up a fresh game instance before each test, with two players,
     * two filled staircase columns, and a draw pile with three cards.
     * */
    @BeforeTest
    fun setUp() {
        p1 = Player("Alice")
        p2 = Player("Bob")

        game = KartentreppeGame(p1, p2)

        c1 = game.staircase[0]
        c2 = game.staircase[1]
        draw = game.drawStack
        discard = game.discardStack

        // Fill the first two staircase columns (top = last)
        c1.addLast(Card(CardValue.SEVEN, CardSuit.HEARTS))
        c1.addLast(Card(CardValue.EIGHT, CardSuit.HEARTS))  // top of c1

        c2.addLast(Card(CardValue.NINE, CardSuit.SPADES))   // top of c2

        // Fill draw pile (top = last)
        draw.addLast(Card(CardValue.JACK,  CardSuit.CLUBS))
        draw.addLast(Card(CardValue.QUEEN, CardSuit.DIAMONDS))
        draw.addLast(Card(CardValue.KING,  CardSuit.CLUBS))
    }

    /** Verifies that a freshly created game starts with all default values
     *  (empty stacks, player1 starts, no removals). */
    @Test
    fun `initial state respects defaults`() {
        val fresh = KartentreppeGame(Player("A"), Player("B"))

        assertSame(fresh.player1, fresh.currentPlayer, "player1 should start")
        assertTrue(fresh.discardStack.isEmpty(), "discard should start empty")
        assertTrue(fresh.log.isEmpty(), "log should start empty")
        assertFalse(fresh.hasRemoved, "hasRemoved should default to false")
        assertEquals(5, fresh.staircase.size, "staircase should have 5 columns by default")
        assertTrue(fresh.drawStack.isEmpty(), "draw stack should start empty")
    }

    /** Checks that the top card of each staircase column is always the last element in the stack. */
    @Test
    fun `staircase top is last element in each column`() {
        val topC1 = c1.last()
        val topC2 = c2.last()
        assertEquals(CardValue.EIGHT, topC1.value)
        assertEquals(CardValue.NINE,  topC2.value)

        val newCard = Card(CardValue.TEN, CardSuit.SPADES)
        c2.addLast(newCard)
        assertSame(newCard, c2.last())
        assertEquals(2, c2.size)
    }

    /** Ensures that drawing a card reduces the draw stack and correctly adds the card to a player's hand. */
    @Test
    fun `drawing from drawStack reduces its size and can move card to a hand`() {
        val before = game.drawStack.size
        val drawn = game.drawStack.removeLast()
        p1.handCards.add(drawn)
        assertEquals(before - 1, game.drawStack.size)
        assertTrue(p1.handCards.contains(drawn))
    }

    /** Verifies that cards added to the discard pile appear at the top and are visible as the last element. */
    @Test
    fun `discard receives cards and top is visible last`() {
        val played = Card(CardValue.ACE, CardSuit.HEARTS)
        game.discardStack.addLast(played)
        assertFalse(game.discardStack.isEmpty())
        assertSame(played, game.discardStack.last())
    }

    /** Confirms that game log entries are recorded in the order they occur. */
    @Test
    fun `log collects messages in order`() {
        game.log.add("Alice drew a card")
        game.log.add("Bob combined ♠9 with ♠10")
        assertEquals(listOf("Alice drew a card", "Bob combined ♠9 with ♠10"), game.log)
    }

    /** Tests that the current player can switch correctly between player1 and player2. */
    @Test
    fun `currentPlayer can switch between player1 and player2`() {
        assertSame(p1, game.currentPlayer)
        game.currentPlayer = p2
        assertSame(p2, game.currentPlayer)
        game.currentPlayer = p1
        assertSame(p1, game.currentPlayer)
    }

    /** Checks that the hasRemoved flag can be set and reset correctly during gameplay. */
    @Test
    fun `hasRemoved flag can be toggled and reset`() {
        assertFalse(game.hasRemoved)
        game.hasRemoved = true
        assertTrue(game.hasRemoved)
        game.hasRemoved = false
        assertFalse(game.hasRemoved)
    }

    /** Ensures that a card cannot exist in multiple containers (draw, hand, discard) at the same time. */
    @Test
    fun `no implicit duplication between containers during simple moves`() {
        val drawn = game.drawStack.removeLast()
        assertFalse(game.drawStack.contains(drawn))
        p1.handCards.add(drawn)
        assertTrue(p1.handCards.contains(drawn))

        p1.handCards.remove(drawn)
        game.discardStack.addLast(drawn)
        assertFalse(p1.handCards.contains(drawn))
        assertTrue(game.discardStack.contains(drawn))
    }

    /** Confirms that changes in one staircase column do not affect other columns (each stack is independent). */
    @Test
    fun `staircase columns are independent stacks`() {
        val c1TopBefore = c1.last()
        val c2TopBefore = c2.last()

        val extra = Card(CardValue.TEN, CardSuit.HEARTS)
        c1.addLast(extra)

        assertSame(extra, c1.last())
        assertSame(c2TopBefore, c2.last()) // c2 unaffected
        assertNotEquals(c1TopBefore, c1.last())
    }
}