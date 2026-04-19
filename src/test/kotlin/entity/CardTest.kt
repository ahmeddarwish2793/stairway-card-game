package entity


import kotlin.test.*

/**
 * Tests for the [Card] class.
 */
class CardTest {

    @Test
    fun cardsWithSameValueAndSuitAreEqual() {
        val c1 = Card(CardValue.JACK, CardSuit.HEARTS)
        val c2 = Card(CardValue.JACK, CardSuit.HEARTS)
        assertEquals(c1, c2)
    }

    @Test
    fun cardsWithDifferentValueOrSuitAreNotEqual() {
        val c1 = Card(CardValue.SEVEN, CardSuit.SPADES)
        val c2 = Card(CardValue.EIGHT, CardSuit.SPADES)
        val c3 = Card(CardValue.SEVEN, CardSuit.CLUBS)
        assertNotEquals(c1, c2)
        assertNotEquals(c1, c3)
    }

    @Test
    fun toStringGivesReadableShortOutput() {
        val c = Card(CardValue.ACE, CardSuit.HEARTS)
        val str = c.toString()
        // Expected: "A♥"
        assertTrue(str.contains("A"))
        assertTrue(str.contains("♥"))
        assertEquals("A♥", str)
    }
}