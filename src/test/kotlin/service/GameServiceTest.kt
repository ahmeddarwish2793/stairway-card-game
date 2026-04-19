package service

import entity.*
import kotlin.test.*

/**
 * Tests for [GameService].
 *
 * POINTS TO CHECK :
 * - Game  invariants (staircase, hands, piles, current player, log).
 * - Valid/invalid compare logic.
 * - Drawing & refilling (refill allowed vs. end-game).
 * - End-game.
 */

class GameServiceTest {
    private lateinit var root: RootService
    private lateinit var gameService: GameService

    /** To create a fresh [RootService] and [GameService] before each test. */
    @BeforeTest
    fun setUp() {
        root = RootService()
        gameService = root.gameService
    }


    // -------------------------------------------------------------------------
    // startGame
    // -------------------------------------------------------------------------

    /**test initial conditions upon creating the game instant*/
    @Test
    fun testStartGame_initialCounts() {
        gameService.startGame("Ahmed", "Aly")
        val g = requireNotNull(root.game)

        /**each player gets 5 cards*/
        assertEquals(5, g.player1.handCards.size)
        assertEquals(5, g.player2.handCards.size)

        /**staircase has 15 cards*/
        val stairTotal = g.staircase.sumOf { it.size }
        assertEquals(15, stairTotal)

        /**remaining cards go to drawStack: 52 - 15 - 10 = 27*/
        assertEquals(27, g.drawStack.size)

        /**discard is empty at start*/
        assertTrue(g.discardStack.isEmpty())

        /**at least a couple of log entries were written*/
        assertTrue(g.log.size >= 2)
    }

    /** Verifies startGame() sets the current player to player1. */
    @Test
    fun testStartGame_currentPlayerIsPlayer1() {
        gameService.startGame("Ahmed", "Aly")
        val g = requireNotNull(root.game)
        assertSame(g.player1, g.currentPlayer)
    }

    /** startGame() rejects blank names via IllegalArgumentException. */
    @Test
    fun testStartGame_rejectsBlankNames() {
        assertFailsWith<IllegalArgumentException> { gameService.startGame("", "Aly") }
        assertFailsWith<IllegalArgumentException> { gameService.startGame("Ahmed", "   ") }
    }

    /** startGame() rejects duplicate names via IllegalArgumentException. */
    @Test
    fun testStartGame_rejectsDuplicateNames() {
        assertFailsWith<IllegalArgumentException> { gameService.startGame("Ahmed", "Ahmed") }
    }


    // -------------------------------------------------------------------------
    // compareCards
    // -------------------------------------------------------------------------

    /** compareCards() throws IllegalStateException if no game is running. */
    @Test
    fun testCompareCards_requiresRunningGame() {
        root.game = null
        assertFailsWith<IllegalArgumentException> {
            gameService.compareCards(
                Card(CardValue.SEVEN, CardSuit.HEARTS),
                Card(CardValue.SEVEN, CardSuit.SPADES)
            )
        }
    }

    /** Returns 0 when the two cards are both hand cards, the player chose 2 handCards
     * instead of one handCard and one stairCard. */
    @Test
    fun testCompareCards_invalidPositions_returns0() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        // both are in hands (no staircase top involved) → invalid
        val h1 = Card(CardValue.SEVEN, CardSuit.HEARTS)
        val h2 = Card(CardValue.SEVEN, CardSuit.DIAMONDS)
        p1.handCards.add(h1)
        p2.handCards.add(h2)

        val res = gameService.compareCards(h1, h2)
        assertEquals(0, res)
    }

    /** Valid combine by same value: points added, cards moved, hasRemoved flagged. */
    @Test
    fun testCompareCards_validCombine_updatesStateAndReturnsPoints() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        val handCard = Card(CardValue.TEN, CardSuit.HEARTS)
        val stairCard = Card(CardValue.TEN, CardSuit.SPADES)
        p1.handCards.add(handCard)
        g.staircase[0].addLast(stairCard)

        val gained = gameService.compareCards(handCard, stairCard)

        /**points: 10 + 10 = 20*/
        assertEquals(20, gained)

        /**cards removed from hand/staircase and added to collectedCards*/
        assertTrue(p1.collectedCards.contains(handCard))
        assertTrue(p1.collectedCards.contains(stairCard))
        assertTrue(g.staircase[0].isEmpty())

        /**hasRemoved must be set*/
        assertTrue(g.hasRemoved)
    }

    /** Valid combine by same suit: also returns points and updates state. */
    @Test
    fun testCompareCards_sameSuitAlsoValid() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        val handCard = Card(CardValue.ACE, CardSuit.HEARTS)   // points = 1
        val stairCard = Card(CardValue.KING, CardSuit.HEARTS) // points = 20
        p1.handCards.add(handCard)
        g.staircase[1].addLast(stairCard)

        val gained = gameService.compareCards(handCard, stairCard)

        assertEquals(1 + 20, gained)
        assertEquals(21, p1.score)
        assertTrue(g.staircase[1].isEmpty())
        assertTrue(p1.collectedCards.contains(handCard))
        assertTrue(p1.collectedCards.contains(stairCard))
        assertTrue(g.hasRemoved)
    }

    /** As the player may click on stairCard or handCard first, I created this logic:
     * (hand, stair) or (stair, hand) both work the same. */
    @Test
    fun testCompareCards_parameterOrderInsensitive() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        val handCard = Card(CardValue.NINE, CardSuit.CLUBS)    // 9
        val stairCard = Card(CardValue.NINE, CardSuit.SPADES)  // 9
        p1.handCards.add(handCard)
        g.staircase[3].addLast(stairCard)

        val a = gameService.compareCards(handCard, stairCard)
        /**As compareCards removed both cards to collectedCards, I am adding them again*/
        p1.handCards.add(handCard)
        g.staircase[3].addLast(stairCard)
        val b = gameService.compareCards(stairCard, handCard)

        assertEquals(18, a)
        assertEquals(18, b)
    }

    // -------------------------------------------------------------------------
    // drawCard / refillDrawStack / endGame
    // -------------------------------------------------------------------------

    /**
     * If the player already has 5 cards, then log and return.
     * We assert that the hand size = 5 stays unchanged.
     */
    @Test
    fun testDrawCard_whenHandIsFull_logsAndReturns() {
        gameService.startGame("Alice", "Bob")
        val g = requireNotNull(root.game)
        val before = g.currentPlayer.handCards.size

        /**With a fresh game, current player already has 5, drawCard should do nothing*/
        gameService.drawCard()

        assertEquals(before, g.currentPlayer.handCards.size)
    }

    /** When draw is empty, discard has cards, and hasRemoved == true, a refill occurs and draw succeeds. */
    @Test
    fun testDrawCard_triggersRefill_whenAllowed() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        /** player is eligible to draw when (4 cards in hand)*/
        p1.handCards.clear()
        repeat(4) { p1.handCards.add(Card(CardValue.TWO, CardSuit.CLUBS)) }

        /**draw empty, discard non-empty, hasRemoved = true, so OK to refill*/
        g.drawStack.clear()
        g.discardStack.addLast(Card(CardValue.THREE, CardSuit.HEARTS))
        g.discardStack.addLast(Card(CardValue.FOUR, CardSuit.SPADES))
        g.hasRemoved = true

        gameService.drawCard()

        assertEquals(5, p1.handCards.size)
        assertTrue(g.discardStack.isEmpty())
        /**reset after refill*/
        assertFalse(g.hasRemoved)
    }

    /**
     * If drawStack is empty and hasRemoved == false, so endGame() is called during refill.
     */
    @Test
    fun testDrawCard_whenNoRemovalSinceShuffle_endGame() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        /**player with 4 cards is eligible to draw*/
        p1.handCards.clear()
        repeat(4) { p1.handCards.add(Card(CardValue.SEVEN, CardSuit.DIAMONDS)) }

        /**drawStack empty, discardStack has cards, hasRemoved == false, then call endGame()*/
        g.drawStack.clear()
        g.discardStack.addLast(Card(CardValue.ACE, CardSuit.CLUBS))
        g.hasRemoved = false

        gameService.drawCard()
        assertNull(root.game)
    }

    /** endGame() clears root.game and logs the outcome (winner or tie). */
    @Test
    fun testEndGame_clearsGameAndLogs() {
        gameService.startGame("A", "B")
        val g = requireNotNull(root.game)
        g.player1.score = 10
        g.player2.score = 3

        gameService.endGame()

        assertNull(root.game)
        assertTrue(g.log.any { it.contains("Winner") || it.contains("tie") })
    }


}