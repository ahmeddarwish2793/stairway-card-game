package entity

import kotlin.test.*

/**
 * Unit tests for the [Player] class.
 *
 * These tests verify that a Player’s properties (name, score, handCards,
 * collectedCards, and hasDestroyed) behave correctly during gameplay,
 * and that updates are reflected as expected in the player's state.
 */
class PlayerTest {

    /** Verifies that a newly created player starts with all default values
     * (score 0, empty lists, no destroyed flag). */
    @Test
    fun `defaults are initialized correctly`() {
        val p = Player("Alice")
        assertEquals("Alice", p.name)
        assertEquals(0, p.score)
        assertTrue(p.handCards.isEmpty(), "handCards should start empty")
        assertTrue(p.collectedCards.isEmpty(), "collectedCards should start empty")
        assertFalse(p.hasDestroyed, "hasDestroyed should start false")
        assertEquals("Alice(score = 0)", p.toString())
    }

    /** Checks that the player's score can be increased and decreased correctly during the game. */
    @Test
    fun `score can be updated`() {
        val p = Player("Bob")
        p.score += 10
        assertEquals(10, p.score)
        p.score -= 3
        assertEquals(7, p.score)
        assertEquals("Bob(score = 7)", p.toString())
    }

    /** Ensures that the player's handCards list can be modified by adding and removing cards. */
    @Test
    fun `handCards can be modified`() {
        val p = Player("Carol")
        val c1 = Card(CardValue.SEVEN, CardSuit.SPADES)
        val c2 = Card(CardValue.JACK, CardSuit.HEARTS)

        // Add cards to the hand
        p.handCards += c1
        p.handCards += c2
        assertEquals(2, p.handCards.size)
        assertTrue(p.handCards.containsAll(listOf(c1, c2)))

        // Remove a card and verify the remaining one
        p.handCards.remove(c1)
        assertEquals(1, p.handCards.size)
        assertEquals(c2, p.handCards.first())
    }

    /** Verifies that the collectedCards list updates when the player collects cards from successful moves. */
    @Test
    fun `collectedCards can be modified`() {
        val p = Player("Dave")
        val won = Card(CardValue.ACE, CardSuit.HEARTS)

        p.collectedCards += won
        assertEquals(1, p.collectedCards.size)
        assertEquals(won, p.collectedCards.first())
    }

    /** Checks that the hasDestroyed flag correctly toggles between true and false when set. */
    @Test
    fun `hasDestroyed flag toggles as expected`() {
        val p = Player("Eve")
        assertFalse(p.hasDestroyed)
        p.hasDestroyed = true
        assertTrue(p.hasDestroyed)
        p.hasDestroyed = false
        assertFalse(p.hasDestroyed)
    }

    /** Ensures that each Player instance is a distinct object, even if two players share the same name and score. */
    @Test
    fun `players with same data are still different objects`() {
        val p1 = Player("Frank")
        val p2 = Player("Frank")

        // Different instances should not be equal (reference inequality)
        assertNotSame(p1, p2, "Players with the same name should be different objects")

        // Updating score should not link objects
        p1.score = 10
        assertNotEquals(p1.score, p2.score, "Score changes should affect only one instance")
    }
}