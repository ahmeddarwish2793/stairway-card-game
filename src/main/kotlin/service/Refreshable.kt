package service

/**
 * This interface provides a mechanism for the service layer classes to communicate
 * (usually to the GUI classes) that certain changes have been made to the entity
 * layer, so that the user interface can be updated accordingly.
 *
 * Default (empty) implementations are provided for all methods, so that implementing
 * GUI classes only need to react to events relevant to them.
 *
 * @see AbstractRefreshingService
 */
interface Refreshable {
    /** Refresh Staircase view after combining or destroying, by receiving the index of the column
     * that should be updated by removing the top card in this column */
    fun refreshStaircase(index: Int) {}

    /** Refresh a player's hand by passing the index of card*/
    fun refreshHand(index: Int) {}

    /** Refresh log with new actions as string*/
    fun refreshLog() {}

    /** Refresh the Face-up discard stack*/
    fun refreshDiscardStack() {}

    /** Refresh the Face-down draw stack*/
    fun refreshDrawStack() {}

    /** Called once after Game start to refresh the whole game scene HandCards, Staircase etc.*/
    fun refreshAfterStartGame() {}

    /** Called after the end of player's turn*/
    fun refreshAfterTurn() {}

    /** called when endGame() finishes the Game*/
    fun refreshToEndGame() {}

    /** refresh both players' scores.
     * i will call after each p.score += or -= */
    fun refreshScore() {}



}