package entity

/**
 * Represents a single playing card in the game.
 *
 * Each card has a [value] (like seven, jack, ace) and a [suit] (like hearts or spades).
 * This class also provides a readable string using [toString].
 *
 * @property value the rank or number of the card (see [CardValue])
 * @property suit the suit of the card (see [CardSuit])
 */
data class Card (val value: CardValue, val suit: CardSuit) {


    //provide a string representation of this card, so it's readable in Log
    override fun toString() = "${value}${suit}"
}