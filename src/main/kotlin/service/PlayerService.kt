package service

import entity.*

/**
 * Here we will implement the 4 player actions
 * - combine a hand card with an open (top) staircase card
 * - discard a hand card to the discard stack
 * - destroy one open staircase card (once per turn, costs the currentPlayer 5 points)
 * - startTurn(): tell the GUI to show the next player's start screen*/
class PlayerService(private val root: RootService) : AbstractRefreshingService() {

    /**
     * Takes a card from the current player’s hand and combines it with a
     * card from the staircase incase they share value or suit.
     * Once triggered, both cards will be removed from their places,
     * points for both cards will be added to the current player’s score
     * and the both same cards will join his collectedCards, and the covered
     * card below the removed staircase card (if any) will be flipped face-up.
     * And the player draw a new card.
     *
     * @param handCard The card chosen from the current player's hand.
     * @param stairCard The top card from the staircase that the player attempts to combine with.
     */
    fun combineCard(handCard: Card, stairCard: Card) {
        val g = requireNotNull(root.game) { "No game running." }

        /**
         * The combination logic is entirely in GameService.compareCards
         * */
        val gainedPoints = root.gameService.compareCards(handCard, stairCard)

        /**
         * If no valid combination, stop — compareCards() already handled logging and refresh
         * */
        if (gainedPoints == 0) return

        /**
         *  Check for end-game condition: staircase empty
         *  */
        if (isStaircaseEmpty(g)) {
            g.log.add("All staircase cards have been removed.")
            onAllRefreshables { refreshLog() }
            root.gameService.endGame()
            return
        }

        /**
         * Otherwise, draw one card to finish the turn
         * */
        root.gameService.drawCard()

        /**
         * If the game is still running after the draw, start the next player's turn
         * */
        if (root.game != null) startTurn()
    }

    /**
     * Discard one card from the current player's hand to the discard stack.
     * @param handCard The card chosen from the current player's hand.
     */
    fun discardCard(handCard: Card) {
        val g = requireNotNull(root.game)
        val p = g.currentPlayer

        /**
         * check if the chosen card is from player's hand
         * */
        if(!p.handCards.contains(handCard)) {
            g.log.add("${p.name}, the discarded card should be from your hand.")
            onAllRefreshables { refreshLog() }
            return
        }
        /**
         * find index of the chosen card in player's hand before removing it
         * move from hand to discard pile*/
        val removedIndex = p.handCards.indexOf(handCard)
        p.handCards.remove(handCard)
        g.discardStack.addLast(handCard)

        /**
         * Log + refresh what changed due to discarding*/
        g.log.add("${p.name} discarded $handCard.")
        onAllRefreshables {
            refreshHand(removedIndex)
            refreshDiscardStack()
            refreshLog()
        }

        /**
         * End the turn by drawing a card*/
        root.gameService.drawCard()

        /**
         * If the game is still running after the draw, switch to the next player*/

        if (root.game != null) startTurn()

    }

    /**
     * Destroy one open (top) staircase card.
     * Only once per turn and only if the player has at least 5 points (cost = 5).
     */
    fun destroyCard(stairCard: Card) {
        val g = requireNotNull(root.game)
        val p = g.currentPlayer

        /**
         * if current player destroyed before, tell him he is not allowed to destroy again*/
        if(p.hasDestroyed) {
            g.log.add("${p.name}, you can't destroy twice in the same round")
            onAllRefreshables { refreshLog() }
            return
        }

        /**
         * if the current player's score is below 5, tell him he requires minimum 5 pts to destroy*/
        if(p.score <5) {
            g.log.add("${p.name}, you need at least 5 points in you score to destroy a card")
            onAllRefreshables { refreshLog() }
            return
        }

        val columnIndex = root.gameService.findColumnOfTopCard(g, stairCard)
        if(columnIndex == -1) {
            g.log.add("${p.name}, you need to choose a top face-up card.")
            onAllRefreshables { refreshLog() }
            return
        }

        /**
         * when conditions are okay, remove card from stairCase and add to discard pile*/
        g.staircase[columnIndex].removeLast()
        g.discardStack.addLast(stairCard)

        /**
         * pay cost and update flags*/
        p.score -= 5
        p.hasDestroyed = true
        g.hasRemoved = true


        g.log.add("${p.name} destroyed $stairCard from column ${columnIndex + 1} (-5 points).")
        onAllRefreshables {
            refreshStaircase(columnIndex)
            refreshDiscardStack()
            refreshScore()
            refreshLog()
        }

        if (isStaircaseEmpty(g)) {
            g.log.add("All staircase cards have been removed.")
            onAllRefreshables { refreshLog() }
            root.gameService.endGame()
            return
        }
    }

    /**
     * The method startTurn() initializes a new turn for the current player. It updates the active player,and prepares
     * the game environment for the next move. This function ensures that all game rules for
     * the beginning of a turn are applied correctly.
     * */
    fun startTurn() {
        val g = requireNotNull(root.game)

        /**
         * switch current player to the other one
         * */
        g.currentPlayer = if (g.currentPlayer === g.player1) g.player2 else g.player1
        g.log.add("It's now ${g.currentPlayer.name}'s turn.")

        /**
         * reset destroy flag for the player whose turn just started*/
        g.currentPlayer.hasDestroyed = false
        onAllRefreshables { refreshAfterTurn() }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /**
     * Helper functions
     * */



    /** True if every staircase column is empty. */
    private fun isStaircaseEmpty(g: KartentreppeGame): Boolean {
        for (column in g.staircase) {
            if (column.isNotEmpty()) return false
        }
        return true
    }
}