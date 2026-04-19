package service

import entity.*
import kotlin.test.*

/**
 * Tests for [PlayerService] actions: combine, discard, destroy, and startTurn.
 */

class PlayerServiceTest {
    private lateinit var root: RootService
    private lateinit var playerService: PlayerService
    private lateinit var gameService: GameService

    /**
     * sets up a fresh RootService with PlayerService and GameService before each test.
     * */
    @BeforeTest
    fun setUp() {
        root = RootService()
        playerService = root.playerService
        gameService = root.gameService
    }

    // --------------------------------------------------------------------
    // combineCard
    // --------------------------------------------------------------------

    /** Combining a valid pair (same value) removes both, scores points, collects, draws, and switches turn. */
    @Test
    fun testCombineCard_valid_updatesScore_collects_draws_and_switches() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        /** match the 2 cards by value TEN; ensure draw has a card to finish the turn*/
        val handCard = Card(CardValue.TEN, CardSuit.HEARTS)
        val stairCard = Card(CardValue.TEN, CardSuit.SPADES)
        p1.handCards.clear()
        /**Making p1 have 5 cards including the matching one so after combine it's 4 (eligible to draw)*/
        p1.handCards.addAll(listOf(
            handCard,
            Card(CardValue.TWO, CardSuit.CLUBS),
            Card(CardValue.THREE, CardSuit.CLUBS),
            Card(CardValue.FOUR, CardSuit.CLUBS),
            Card(CardValue.FIVE, CardSuit.CLUBS)
        ))
        g.staircase[0].addLast(stairCard)
        /**I added this Card to ensure the staircase is NOT empty after the combine,
         * to avoid ending the game and to check switch turn*/
        g.staircase[1].addLast(Card(CardValue.ACE, CardSuit.CLUBS))
        g.drawStack.addLast(Card(CardValue.ACE, CardSuit.DIAMONDS))

        playerService.combineCard(handCard, stairCard)

        assertEquals(20, p1.score, "Score should increase by 10 + 10.")
        assertTrue(p1.collectedCards.containsAll(listOf(handCard, stairCard)))
        assertTrue(g.staircase[0].isEmpty(), "Staircase column should be emptied at the top.")
        assertSame(p2, g.currentPlayer, "Turn should pass to the other player after combine+draw.")
    }

    /** Invalid combine (no suit/value match) should not change anything nor draw. */
    @Test
    fun testCombineCard_invalidNoMatch_noChange_noDraw() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        val hand = Card(CardValue.TWO, CardSuit.CLUBS)
        val top = Card(CardValue.THREE, CardSuit.SPADES)
        p1.handCards.clear()
        p1.handCards.addAll(listOf(
            hand,
            Card(CardValue.FOUR, CardSuit.HEARTS),
            Card(CardValue.FIVE, CardSuit.HEARTS),
            Card(CardValue.SIX, CardSuit.HEARTS),
            Card(CardValue.SEVEN, CardSuit.HEARTS)
        ))
        g.staircase[0].addLast(top)

        val drawBefore = g.drawStack.size
        val scoreBefore = p1.score
        val handBefore = p1.handCards.toList()

        playerService.combineCard(hand, top)

        assertEquals(drawBefore, g.drawStack.size, "Should not draw on invalid combine.")
        assertEquals(scoreBefore, p1.score, "Score should not change.")
        assertEquals(handBefore, p1.handCards, "Hand should not change.")
        assertEquals(top, g.staircase[0].last(), "Top should remain the same on invalid combine.")
        assertSame(p1, g.currentPlayer, "Turn should not switch on invalid combine.")
    }

    /** Combining the final card in stairCase empties the stairCase and ends the game. */
    @Test
    fun testCombineCard_emptiesStaircase_endsGame() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        /**Empty staircase except one column with a single top card that matches hand*/
        g.staircase.forEach { it.clear() }
        val top = Card(CardValue.KING, CardSuit.HEARTS)
        val hand = Card(CardValue.KING, CardSuit.SPADES)
        g.staircase[0].addLast(top)
        p1.handCards.clear()
        /**keep 5 cards including the match so draw would be attempted, but game ends first*/
        p1.handCards.addAll(listOf(
            hand,
            Card(CardValue.TWO, CardSuit.CLUBS),
            Card(CardValue.THREE, CardSuit.CLUBS),
            Card(CardValue.FOUR, CardSuit.CLUBS),
            Card(CardValue.FIVE, CardSuit.CLUBS)
        ))
        /**drawStack has cards, but endGame triggers first due to empty staircase*/
        g.drawStack.addLast(Card(CardValue.ACE, CardSuit.DIAMONDS))

        playerService.combineCard(hand, top)

        assertNull(root.game, "Game should end when staircase becomes empty.")
    }

    /** Trying to combine with a non-top staircase card should fail and not draw. */
    @Test
    fun testCombineCard_rejectsHiddenStairCard() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        val covered = Card(CardValue.NINE, CardSuit.HEARTS)
        val top = Card(CardValue.QUEEN, CardSuit.CLUBS)
        val hand = Card(CardValue.NINE, CardSuit.SPADES)
        g.staircase[1].addLast(covered)
        g.staircase[1].addLast(top)
        p1.handCards.clear()
        p1.handCards.addAll(listOf(
            hand,
            Card(CardValue.TWO, CardSuit.CLUBS),
            Card(CardValue.THREE, CardSuit.CLUBS),
            Card(CardValue.FOUR, CardSuit.CLUBS),
            Card(CardValue.FIVE, CardSuit.CLUBS)
        ))
        val drawBefore = g.drawStack.size

        /**Attempt combine with the covered one (invalid)*/
        playerService.combineCard(hand, covered)

        assertEquals(drawBefore, g.drawStack.size, "No draw should happen on invalid combine.")
        assertEquals(top, g.staircase[1].last(), "Top card should remain unchanged.")
        assertSame(p1, g.currentPlayer, "Turn should not switch on invalid combine.")
    }


    // --------------------------------------------------------------------
    // discardCard
    // --------------------------------------------------------------------

    /** Discarding a card moves it to discard, draws one, and keeps hand size at 5. */
    @Test
    fun testDiscardCard_movesCardToDiscard_and_draws() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        /**Prepare hand with 5 cards and a drawStack with one card*/
        val toDiscard = Card(CardValue.FIVE, CardSuit.DIAMONDS)
        p1.handCards.clear()
        p1.handCards.addAll(listOf(
            Card(CardValue.TWO, CardSuit.CLUBS),
            Card(CardValue.THREE, CardSuit.CLUBS),
            Card(CardValue.FOUR, CardSuit.CLUBS),
            Card(CardValue.SIX, CardSuit.CLUBS),
            toDiscard
        ))

        val drawCard = Card(CardValue.ACE, CardSuit.SPADES)
        g.drawStack.clear()
        g.drawStack.addLast(drawCard)

        val initialDrawSize = g.drawStack.size

        playerService.discardCard(toDiscard)

        assertEquals(1, g.discardStack.size, "Discard should contain the discarded card.")
        assertTrue(g.discardStack.contains(toDiscard))

        assertEquals(initialDrawSize - 1, g.drawStack.size, "Draw stack should decrease by one.")
        assertEquals(5, p1.handCards.size, "Hand size should be restored to 5 after drawing.")
    }

    /** Discarding a card not in hand should only log and not change hand/discard. */
    @Test
    fun testDiscardCard_rejectsForeignCard_logsAndNoChange() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        /**P1 has 5 cards, but tries to discard a foreign one*/
        p1.handCards.clear()
        repeat(5) { p1.handCards.add(Card(CardValue.entries[it], CardSuit.CLUBS)) }
        val foreign = Card(CardValue.ACE, CardSuit.HEARTS)

        val discardBefore = g.discardStack.size
        val handBefore = p1.handCards.toList()
        playerService.discardCard(foreign)

        assertEquals(discardBefore, g.discardStack.size, "No new card should be discarded.")
        assertEquals(handBefore, p1.handCards, "Hand should remain unchanged.")
    }

    /** Discard should end the turn: after drawing, startTurn() switches to the other player. */
    @Test
    fun testDiscardCard_switchesTurnAfterDraw() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        /**5 cards in hand, at least 1 in draw to allow draw step*/
        val toDiscard = Card(CardValue.NINE, CardSuit.SPADES)
        p1.handCards.clear()
        p1.handCards.addAll(listOf(
            Card(CardValue.TWO, CardSuit.CLUBS),
            Card(CardValue.THREE, CardSuit.CLUBS),
            Card(CardValue.FOUR, CardSuit.CLUBS),
            Card(CardValue.FIVE, CardSuit.CLUBS),
            toDiscard
        ))
        g.drawStack.addLast(Card(CardValue.ACE, CardSuit.CLUBS))

        playerService.discardCard(toDiscard)

        assertSame(p2, g.currentPlayer, "Turn should pass to the other player after discard+draw.")
    }

    // --------------------------------------------------------------------
    // destroyCard
    // --------------------------------------------------------------------

    /** Destroying a top card removes it, moves it to discard, deducts 5 points, and sets flags. */
    @Test
    fun testDestroyCard_removesStaircase_and_paysCost() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        // give player sufficient score and prepare staircase top
        p1.score = 10
        val stairCard = Card(CardValue.SEVEN, CardSuit.HEARTS)
        g.staircase[2].addLast(stairCard)

        playerService.destroyCard(stairCard)

        assertTrue(g.discardStack.contains(stairCard))
        assertEquals(5, p1.score, "Destroying should deduct 5 points.")
        assertTrue(p1.hasDestroyed, "Flag hasDestroyed should be set for this turn.")
        assertTrue(g.hasRemoved, "Global hasRemoved should be set (affects refill rule).")
        assertTrue(g.staircase[2].isEmpty())
    }

    /** A player cannot destroy twice in the same turn. */
    @Test
    fun testDestroyCard_cannotDestroyTwiceInSameTurn() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        p1.score = 10
        val c1 = Card(CardValue.TWO, CardSuit.CLUBS)
        val c2 = Card(CardValue.THREE, CardSuit.CLUBS)
        g.staircase[0].addLast(c1)
        g.staircase[1].addLast(c2)

        playerService.destroyCard(c1)
        val scoreAfterFirst = p1.score

        // Try to destroy again in same turn
        playerService.destroyCard(c2)

        assertEquals(scoreAfterFirst, p1.score, "Second destroy in same turn should be ignored.")
        assertTrue(g.staircase[1].isNotEmpty(), "Second destroy should not remove another card.")
    }

    /** A player with < 5 points cannot destroy. */
    @Test
    fun testDestroyCard_requiresAtLeastFivePoints() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        p1.score = 4
        val stairCard = Card(CardValue.ACE, CardSuit.SPADES)
        g.staircase[0].addLast(stairCard)

        playerService.destroyCard(stairCard)

        assertEquals(4, p1.score, "Score unchanged when not enough points.")
        assertFalse(p1.hasDestroyed)
        assertTrue(g.staircase[0].contains(stairCard), "Card should remain.")
    }

    /** Destroying a non-top (covered) card should fail. */
    @Test
    fun testDestroyCard_rejectsNonTopCard() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        p1.score = 10
        val covered = Card(CardValue.FIVE, CardSuit.HEARTS)
        val top = Card(CardValue.SIX, CardSuit.HEARTS)
        g.staircase[3].addLast(covered)
        g.staircase[3].addLast(top)

        playerService.destroyCard(covered)

        assertTrue(g.staircase[3].contains(covered), "Covered should remain.")
        assertTrue(g.staircase[3].contains(top), "Top should remain.")
        assertEquals(10, p1.score, "No points deducted.")
        assertFalse(p1.hasDestroyed)
    }

    /** Destroying the last remaining staircase card ends the game. */
    @Test
    fun testDestroyCard_emptiesStaircase_endsGame() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        // Only one top card remains across all columns
        g.staircase.forEach { it.clear() }
        val lastTop = Card(CardValue.QUEEN, CardSuit.SPADES)
        g.staircase[4].addLast(lastTop)
        p1.score = 20 // enough to destroy

        playerService.destroyCard(lastTop)

        assertNull(root.game, "Game should end when staircase becomes empty after destroy.")
    }

    // --------------------------------------------------------------------
    // startTurn
    // --------------------------------------------------------------------

    /** startTurn() switches current player and resets hasDestroyed on the new current player. */
    @Test
    fun testStartTurn_switchesPlayer_and_resetsDestroyFlag() {
        val p1 = Player("P1")
        val p2 = Player("P2")
        p1.hasDestroyed = true // irrelevant; will become inactive
        p2.hasDestroyed = true // will be reset when his turn starts

        val g = KartentreppeGame(p1, p2)
        root.game = g
        g.currentPlayer = p1

        playerService.startTurn()

        assertSame(p2, g.currentPlayer, "Turn should switch to the other player.")
        assertFalse(p2.hasDestroyed, "New current player's destroy flag should be reset.")
    }

}