package service

import entity.*

/**
 * High-level game orchestration: startGame, compareCards, drawCard, refillDrawStack and endGame.
 */
class GameService(private val root: RootService): AbstractRefreshingService() {

    /**
     * General description:
     * Starts a new game. The parameters player1Name and player2Name specify the names of the two players.
     * Preconditions:
     * None.
     *
     * Postconditions:
     * The CardStaircase is completely filled.
     * The cards have been dealt.
     * The collected cards are empty.
     * The discard stack is empty.
     * The draw stack is filled.
     * It is the first player's turn.
     * The game field and UI are refreshed to reflect the initialized state.
     *
     * @param player1Name Name of the first player.
     * @param player2Name Name of the second player.
     *
     * Valid value range:
     * Both names must be non-empty strings.
     * Return value:
     * No return value (Unit).
     * Exceptions:
     *
     *
     * IllegalArgumentException: if player1Name or player2Name is empty.
     *
     */
    fun startGame(player1Name: String, player2Name: String) {
        /**
         * make sure that the names are not empty
         * names are not the same*/
        if (player1Name.isBlank() || player2Name.isBlank()) {
            throw IllegalArgumentException("Names must not be blank.")
        }

        if (player1Name == player2Name) {
            throw IllegalArgumentException("Player names must be different.")
        }

        /**
         * root.game is pointing to the single point of truth [which is the current running game]
         * that I created and made game variable point on it
         * */
        val p1 = Player(player1Name)
        val p2 = Player(player2Name)
        val game = KartentreppeGame(p1, p2)
        root.game = game

        /**Step1: Create and shuffle full deck (52 cards)*/
        val fullDeck = mutableListOf<Card>()
        for(suit in CardSuit.entries) {
            for(value in CardValue.entries) {
                fullDeck.add(Card(value, suit))
            }
        }
        fullDeck.shuffle()

        /** Step 2: Create the staircase
         * column 0: 5 cards
         * column 1: 4 cards
         * column 2: 3 cards
         * column 3: 2 cards
         * column 4: 1 card
         */
        var index = 0
        for (column in 0 until game.staircase.size) {
            val cardsInThisColumn = game.staircase.size - column   // 5,4,3,2,1
            repeat(cardsInThisColumn) {
                game.staircase[column].addLast(fullDeck[index++])
            }
        }

        /**Step3: Give each player 5 cards*/
        repeat(5) {
            p1.handCards.add(fullDeck[index++])
            p2.handCards.add(fullDeck[index++])
        }

        /**Step4: Remaining cards in fullDeck, I put them in drawStack*/
        while (index <fullDeck.size) {
            game.drawStack.addLast(fullDeck[index++])
        }

        /**Step5: write in log and update GUI*/
        game.log.add("A new game has started between ${p1.name} and ${p2.name}.")
        game.log.add("${p1.name} begins the game.")
        /**Here I will implement refreshAfterStartGame in mainMenuScene to delete the names written
         * so next time new players want to play they can find the name boxes empty,
         *
         * In GameScene it will refresh:
         * Hands
         * discardStack
         * drawStack
         * staircase
         * log
         * Score
         * */
        onAllRefreshables { refreshAfterStartGame() }


    }

    /**
     * Checks whether two cards can be legally combined and performs the combine if valid.
     *
     * ## General Description
     * This method compares two cards according to the rules of the game.
     * A combination is valid if the two cards either have the **same value** or the **same suit**.
     * One of the cards must belong to the current player's hand,
     * and the other must be the **top card** of one of the staircase columns.
     * @param card1 A [Card] object either from the player's hand or from the staircase (top card).
     * @param card2 A [Card] object either from the player's hand or from the staircase (top card).
     *
     * ## Return Value
     * Returns the total points gained (sum of both cards' values) if the combination is valid,
     * or `0` if the combination is invalid.
     *
     * ## Exceptions
     * @throws IllegalStateException If no game is currently running.
     */
    internal fun compareCards(card1: Card, card2: Card): Int {
        val g = requireNotNull(root.game) { "Game not running." }
        val p = g.currentPlayer

        /**
         * Identify which card belongs to the player's hand and which is the top staircase card.
         * The order of the two parameters does not matter.
         */
        var handCard: Card? = null
        var stairCard: Card? = null
        var stairColumnIndex = -1

        /** Try interpreting card1 as a hand card and card2 as a top staircase card. */
        if (p.handCards.contains(card1)) {
            val idx = findColumnOfTopCard(g, card2)
            if (idx != -1) {
                handCard = card1
                stairCard = card2
                stairColumnIndex = idx
            }
        }

        /** If not valid yet, try the opposite interpretation. */
        if (handCard == null) {
            if (p.handCards.contains(card2)) {
                val idx = findColumnOfTopCard(g, card1)
                if (idx != -1) {
                    handCard = card2
                    stairCard = card1
                    stairColumnIndex = idx
                }
            }
        }

        /** If the cards are not in valid positions, log a message and return 0. */
        if (handCard == null || stairCard == null) {
            g.log.add("${p.name} tried to combine invalid cards " +
                    "(one must be in hand and the other must be the top staircase card).")
            onAllRefreshables { refreshLog() }
            return 0
        }

        /**
         * Check whether the two cards can be combined.
         * They must share the same value or the same suit.
         */
        val isMatch = (handCard.value == stairCard.value) || (handCard.suit == stairCard.suit)
        if (!isMatch) {
            g.log.add("${p.name} tried to combine " +
                    "${handCard.toString()} with ${stairCard.toString()} — not a valid match.")
            onAllRefreshables { refreshLog() }
            return 0
        }

        /**
         * Valid combination:
         * first thing save the index of the handCard in player's hand,
         * so we can pass it to refreshHand later
         * Remove both cards, add them to the player's collected cards,
         * increase the player's score, and mark a staircase card as removed.
         */
        val removedHandIndex = p.handCards.indexOf(handCard)
        p.handCards.remove(handCard)
        g.staircase[stairColumnIndex].removeLast()
        p.collectedCards.add(handCard)
        p.collectedCards.add(stairCard)
        val gained = pointsOf(handCard.value) + pointsOf(stairCard.value)
        p.score += gained
        g.hasRemoved = true

        /** Add a descriptive log message and refresh. */
        g.log.add("${p.name} combined $handCard with $stairCard " +
                "from column ${stairColumnIndex + 1} and gained (+$gained) points.")
        g.log.add("${p.name}'s new score: ${p.score} points.")
        onAllRefreshables {
            refreshHand(removedHandIndex)
            refreshStaircase(stairColumnIndex)
            refreshScore()
            refreshLog()
        }

        /** Return the number of points gained from this combination. */
        return gained
    }

    /**
     * A player gets a new card from the draw pile. If the draw pile is empty,
     * the discard pile is shuffled and added to the draw pile.
     *
     * ## Preconditions
     * - A game is running.
     * - The current player has fewer than 5 cards in hand (typically 4 after combine/discard).
     *
     * ## Postconditions
     * - The current player has exactly 5 cards in hand.
     * - The draw pile is not empty (unless the game ended during refill).
     *
     * ## Exceptions
     * - IllegalStateException: if no game is running.
     * - IllegalStateException: if the player already has 5 hand cards.
     */
    internal fun drawCard() {
        val g = requireNotNull(root.game) { "No game running." }
        val p = g.currentPlayer

        /**
         * Precondition: player must have lost a card before drawing
         * Q#: I am confused here, should I use (IllegalStateException), which may crash the whole program,
         * or just log the error and skip the draw?
         * */
        if (p.handCards.size == 5) {
            g.log.add("Internal error: ${p.name} already has 5 hand cards when attempting to draw. Skipping draw.")
            onAllRefreshables { refreshLog() }
            return
            //throw IllegalStateException("Player already has 5 hand cards.")
        }

        /**
         * Ensure we have something to draw; try to refill if needed
         * */
        if (g.drawStack.isEmpty()) {
            refillDrawStack()

            /**
             * Game might have ended inside refill.
             * */
            root.game ?: return
        }

        // Draw exactly one card
        val drawnCard = g.drawStack.removeLast()
        p.handCards.add(drawnCard)

        // Log + refresh
        g.log.add("${p.name} drew a card.")
        onAllRefreshables {
            refreshDrawStack()
            /**#Q: do we need that in draw or it will be added to the end of the MutableList of handCard?
             *  full refresh for current player's hand after draw, use -1
             * so we can check that in GameScene when implementing this refreshHand*/
            refreshHand(-1)
            refreshLog()
        }
    }


    /**
     * General description:
     * Shuffles all cards on the discard pile and moves them into the draw pile.
     *
     * Preconditions:
     * The draw pile must be empty.
     * There must be at least one card on the discard pile.
     *
     * Postconditions:
     * The refresh methods for draw and discard piles are executed.
     *
     * Return value:
     * None.
     *
     *
     * Exceptions:
     * IllegalStateException: Game not started.
     * IllegalStateException: Draw pile not empty.
     * IllegalStateException: Discard pile empty.
     * */
    private fun refillDrawStack() {
        val game = requireNotNull(root.game)

        //I think it's not logical to check if discardStack is empty here, because
        // it wouldn't be empty if drawStack is empty.
//        /**
//         * if DiscardStack is empty, then endGame
//         * */
//        if(game.discardStack.isEmpty()) {
//            game.log.add("DiscardStack is empty [Game will end]")
//            onAllRefreshables { refreshLog() }
//            endGame()
//            return
//        }

        /**
         * DiscardStack exists, but hasRemoved = false
         * */
        if(!game.hasRemoved) {
            game.log.add("No staircase cards were removed since last shuffle [Game will end]")
            onAllRefreshables { refreshLog() }
            endGame()
            return
        }

        /**
         * endGame conditions are not met, so refill*/
        val temp = game.discardStack.toMutableList()
        game.discardStack.clear()
        temp.shuffle()
        for(card in temp) {
            game.drawStack.addLast(card)
        }

        /**
         * reset hasRemoved flag for the next refill*/
        game.hasRemoved = false
        game.log.add("drawStack is refilled")
        onAllRefreshables {
            refreshLog()
            refreshDrawStack()
            refreshDiscardStack()
        }
    }

    /**
     * General description:
     * Determines the player with the highest score and declares the winner (or a draw if equal).
     * The end screen is then displayed.
     *
     *
     * Preconditions:
     * The last card from the staircase was removed, or
     * The last card from the draw pile was drawn and no card was removed from the staircase since the last shuffle.
     * Note: Currently, no explicit method exists to check whether the staircase is empty —
     * this condition must be logically ensured via game state tracking.
     *
     * Postconditions:
     * The end screen is refreshed and displays the winner, collected cards, and scores.
     *
     * Return value:
     * None.
     *
     * Exceptions:
     * IllegalStateException: Game not started.
     * IllegalStateException: End-game condition not met.
     * */
    internal fun endGame() {
        val game = requireNotNull(root.game)

        val winner = if(game.player1.score > game.player2.score) {
            game.player1.name
        } else if(game.player2.score > game.player1.score) {
            game.player2.name
        } else "it's a tie"


        // store an instance before clearing game
        root.storeResult(
            GameResult(
                player1Name = game.player1.name,
                player2Name = game.player2.name,
                player1Score = game.player1.score,
                player2Score = game.player2.score,
                player1Collected = game.player1.collectedCards.toList(),
                player2Collected = game.player2.collectedCards.toList(),
                winnerName = winner
            )
        )

        game.log.add("Game has ended. ${game.player1.name}: " +
                "${game.player1.score} points, ${game.player2.name}: " +
                "${game.player2.score} points.")
        game.log.add("Winner: $winner!")

        onAllRefreshables { refreshLog() }
        onAllRefreshables { refreshToEndGame() }
        root.game = null

    }

    ////////////////////////////////////////////////////////////////////////

    /**
     * extra functions that helped me*/

    /**
     * Return the index of the column whose TOP card equals [card].
     * If [card] is not an open-top card, return null.
     */
    internal fun findColumnOfTopCard(g: KartentreppeGame, card: Card): Int {
        for (i in g.staircase.indices) {
            val column = g.staircase[i]
            if (column.isNotEmpty()) {
                val top = column.last()
                if (top == card) return i
            }
        }
        return -1
    }


    /**
     * Card points converter to calculate score*/
    private fun pointsOf(v: CardValue): Int {
        return when (v) {
            CardValue.TWO   -> 2
            CardValue.THREE -> 3
            CardValue.FOUR  -> 4
            CardValue.FIVE  -> 5
            CardValue.SIX   -> 6
            CardValue.SEVEN -> 7
            CardValue.EIGHT -> 8
            CardValue.NINE  -> 9
            CardValue.TEN   -> 10
            CardValue.JACK  -> 10
            CardValue.QUEEN -> 15
            CardValue.KING  -> 20
            CardValue.ACE   -> 1
        }
    }
}