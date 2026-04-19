package gui

import entity.*
import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.container.CardStack
import tools.aqua.bgw.components.container.LinearLayout
import tools.aqua.bgw.components.gamecomponentviews.CardView
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.util.BidirectionalMap
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.UIComponent
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.components.uicomponents.TextArea
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.Font
import java.util.Timer
import kotlin.concurrent.schedule





/**
 * The main game scene where the gameplay takes place.
 *
 * @property app The main application instance.
 * @property rootService The root service instance for accessing game logic and data.
 */
class GameScene(val app: SopraApplication, val rootService: RootService):
    BoardGameScene(1920, 1080), Refreshable {

    // so I can call functions of backImage and frontImage from CardImageLoader class
    private val imageLoader = CardImageLoader()

    // As in Maumau game, I am using a BidirectionalMap to map Cards to their corresponding CardViews
    private val cardViews = BidirectionalMap<Card, CardView>()

    // currently selected cards, null if no card is selected
    private var selectedHandCard: Card? = null
    private var selectedStairCard: Card? = null

    // currently selected CardViews, null if no card is selected
    private var selectedHandView: CardView? = null
    private var selectedStairView: CardView? = null
    // last index i stopped at in log
    private var lastLogIndex = 0

    private val cardWidth = 85.0
    private val cardHeight = 130.0

    // staircase layout constants
    private val staircaseLeftX = 720.0           // move to the right
    private val staircaseTopY = 140.0            // overall top of staircase area
    private val staircaseBottomY = 850.0         // Y of the bottom cards' baseline
    private val staircaseColumnGap = cardWidth + 15.0
    private val staircaseVerticalGap = 20.0      // space between cards in a column


    // loop to create stairCase card views
    // CardStack is inside container in BGW and used in Maumau game
    // CardStack didn't fit my need, so I used LinearLayout to create stairCase rows
    // one vertical column per staircase column
    // one vertical pane per staircase column
    private val staircaseColumns: List<Pane<CardView>> = List(5) { col ->
        Pane<CardView>(
            posX = staircaseLeftX + col * staircaseColumnGap,
            posY = staircaseTopY,
            width = cardWidth,
            height = 5 * (cardHeight + staircaseVerticalGap), // enough height for max 5 cards
        )
    }

    // draw stack is a collection(CardStack) of CardView
    private val drawStack = CardStack<CardView>(
        posX = 1500,
        posY = 250,
        width = cardWidth,
        height = cardHeight,
        alignment = Alignment.CENTER
    )

    // discard stack is a collection(CardStack) of CardView
    private val discardStack = CardStack<CardView>(
        posX = 1500,
        posY = 500,
        width = cardWidth,
        height = cardHeight,
        alignment = Alignment.CENTER
    )

    private val logArea = TextArea(
        posX = 0, posY = 0,
        width = 350, height = 400,
        text = ""
    ).apply {
        font = Font(size = 16, color = Color.BLACK)
    }


    // background for log area in left corner, Pane is LayoutView in BGW to hold other components inside it
    private val logBackground = Pane<UIComponent>(
        posX = 100,
        posY = 250,
        width = 350,
        height = 400,
        visual = ColorVisual(230, 230, 230) //light gray background
    ).apply {
        add(logArea)
    }


    // player info(name and score of current player) in right bottom corner
    private val playerNameLabel = Label(
        posX = 1600,
        posY = 850,
        width = 200,
        height = 40,
        text = "",
        alignment = Alignment.CENTER,
        visual = ColorVisual(220, 220, 220) //light gray background
    )

    private val scoreLabel = Label(
        posX = 1600,
        posY = 900,
        width = 200,
        height = 40,
        text = "",
        alignment = Alignment.CENTER,
        visual = ColorVisual(220, 220, 220) //light gray background
    )

    // current player's hand card Layout using liner layout
    private val playerHandLayout = LinearLayout<CardView>(
        posX = 460,
        posY = 850,
        width = 1000,
        height = 200,
        spacing = 10,
        alignment = Alignment.CENTER
    )

    // opponent player's hand card Layout using liner layout
    private val opponentHandLayout = LinearLayout<CardView>(
        posX = 460,
        posY = 50,
        width = 1000,
        height = 200,
        spacing = -60,
        alignment = Alignment.CENTER
    )


    // creating my 3 action buttons: combine, discard, destroy
    //combine button
    private val combineButton = Button(
        posX = 100,
        posY = 700,
        width = 150,
        height = 50,
        text = "Combine",
        visual = ColorVisual(180, 255, 180) // light green background
    )

    //discard button
    private val discardButton = Button(
        posX = 270,
        posY = 700,
        width = 150,
        height = 50,
        text = "Discard",
        visual = ColorVisual(255, 220, 180) // light orange background
    )

    //destroy button
    private val destroyButton = Button(
        posX = 440,
        posY = 700,
        width = 150,
        height = 50,
        text = "Destroy",
        visual = ColorVisual(255, 150, 150) // light red background
    )

    // init block to setup the scene
    init {
        // set background color for the game scene
        background = ColorVisual(0, 128, 0) // Dark green background

        addComponents(opponentHandLayout)
        staircaseColumns.forEach { addComponents(it) }


        // add log label to log background pane

        // add all components to the scene
        addComponents(
            drawStack,
            discardStack,
            logBackground,
            playerNameLabel,
            scoreLabel,
            playerHandLayout,
            combineButton,
            discardButton,
            destroyButton
        )

        // Adding button click listeners
        combineButton.onMouseClicked = {
            val handCard = selectedHandCard
            val stairCard = selectedStairCard
            if (handCard != null && stairCard != null) {
                rootService.playerService.combineCard(handCard, stairCard)
                // Clear selections after action
                selectedHandCard = null
                selectedStairCard = null
            }
            clearSelections()
        }

        discardButton.onMouseClicked = {
            val handCard = selectedHandCard
            if (handCard != null) {
                rootService.playerService.discardCard(handCard)
                // Clear selection after action
                selectedHandCard = null
            }
            clearSelections()
        }

        destroyButton.onMouseClicked = {
            val stairCard = selectedStairCard
            if (stairCard != null) {
                rootService.playerService.destroyCard(stairCard)
                // Clear selection after action
                selectedStairCard = null
            }
            clearSelections()
        }
    }

    // Refresh methods to update the game scene based on game state changes
    // Refesh after starting a new game
    override fun refreshAfterStartGame() {
        val g = requireNotNull(rootService.game)

        // reset log tracking variables
        lastLogIndex = 0
        logArea.text = ""   // clear old log completely
        clearSelections()

        // create CardViews and save them in the bidirectional map, but clear first the map from previous game
        cardViews.clear()
        for (suit in CardSuit.entries) {
            for (value in CardValue.entries) {
                val card = Card(value, suit)
                cardViews[card] = CardView(
                    width = cardWidth,
                    height = cardHeight,
                    front = imageLoader.frontImageFor(suit, value),
                    back = imageLoader.backImage
                )
            }
        }

        // set player name label
        playerNameLabel.text = "Player: ${g.currentPlayer.name}"
        //set initial score
        scoreLabel.text = "Score: ${g.currentPlayer.score}"

        // refresh all components
        refreshStaircase(-1)
        refreshHand(-2) // -2 means refresh both players' hands at game start
        refreshDiscardStack()
        refreshDrawStack()
        refreshScore()
        refreshLog()
    }

    /**
     * Refresh stairCase columns, if columnIndex is -1 refresh all columns(Game is starting),
     * else refresh only the given column
     * */
    override fun refreshStaircase(index: Int) {
        val g = requireNotNull(rootService.game)

        if (index == -1) {
            for (colIndex in g.staircase.indices) {
                refreshSingleColumn(colIndex, g)
            }
        } else {
            refreshSingleColumn(index, g)
        }
    }

    /**
     * Help function to refresh a single staircase column
     * */
    private fun refreshSingleColumn(colIndex: Int, g: KartentreppeGame) {
        val columnPane = staircaseColumns[colIndex]
        val colCards = g.staircase[colIndex]

        columnPane.clear()
        if (colCards.isEmpty()) return

        // I want to calculate the local Y of the TOP edge of the bottom card inside this pane
        val bottomTopLocalY = staircaseBottomY - cardHeight - columnPane.posY

        for (i in colCards.indices) {
            val card = colCards[i]
            val view = cardViews[card]

            // i = 0 : bottom card, i = 1 : one above bottom, and so on
            view.posX = 0.0
            view.posY = bottomTopLocalY - i * (cardHeight + staircaseVerticalGap)

            // GAME LOGIC: last element in column is the top card -> face UP
            if (i == colCards.lastIndex) {
                view.showFront()
            } else {
                view.showBack()
            }

            view.onMouseClicked = {
                val oldView = selectedStairView      // local copy

                if (oldView != null && oldView != view) {
                    unhighlight(oldView)            // no !! because of the warning
                }

                selectedStairCard = card
                selectedStairView = view
                highlight(view)
            }

            columnPane.add(view)
        }
    }

    // Refresh current player's hand cards
    override fun refreshHand(index: Int) {
        val g = requireNotNull(rootService.game)
        val player = g.currentPlayer
        val playerHandView = playerHandLayout
        val opponentHandView = opponentHandLayout

        if (index == -2) {
            // refresh both players' hands at game start
            playerHandView.clear()
            opponentHandView.clear()

            for (card in player.handCards) {
                val view = cardViews[card]
                view.showFront()
                view.onMouseClicked = {
                    // save old selection in a local variable
                    val oldHandView = selectedHandView

                    // unhighlight ONLY old hand selection
                    if (oldHandView != null && oldHandView != view) {
                        unhighlight(oldHandView)
                    }

                    // select + highlight new hand card
                    selectedHandCard = card
                    selectedHandView = view
                    highlight(view)
                }
                playerHandView.add(view)
            }

            repeat(5) {
                val back = CardView(
                    width = cardWidth,
                    height = cardHeight,
                    front = imageLoader.blankImage,
                    back = imageLoader.backImage)
                back.showBack()
                opponentHandView.add(back)
            }
        }
         else {
            playerHandView.clear()

            for (card in player.handCards) {
                val view = cardViews[card]
                view.showFront()
                view.onMouseClicked = {
                    // save old selection
                    val oldHandView = selectedHandView

                    // unhighlight ONLY old hand selection
                    if (oldHandView != null && oldHandView != view) {
                        unhighlight(oldHandView)
                    }

                    // select + highlight new hand card
                    selectedHandCard = card
                    selectedHandView = view
                    highlight(view)
                }
                playerHandView.add(view)
            }
        }
    }



    // Refresh discard stack to show the top card
    override fun refreshDiscardStack() {
        val g = requireNotNull(rootService.game)
        val stack = discardStack

        // clear current visual stack
        stack.clear()

        // if discard stack is empty, show a single placeholder card (back) so the area is visible
        if (g.discardStack.isEmpty()) {
            val placeholder = CardView(
                width = cardWidth,
                height = cardHeight,
                front = imageLoader.blankImage,
                back = imageLoader.backImage
            )
            placeholder.showFront()
            stack.add(placeholder)
            return
        }


        // if not empty, show last card face up
        val topCard = g.discardStack.last()
        println("Top discard card: $topCard") // for testing
        val view = CardView(
            width = cardWidth,
            height = cardHeight,
            front = imageLoader.frontImageFor(topCard.suit, topCard.value),
            back = imageLoader.backImage
        )
        view.showFront()
        stack.add(view)
        println("Discard stack refreshed with top card: $topCard")
    }


    // Refresh draw stack to show back of cards
    override fun refreshDrawStack() {
        val g = requireNotNull(rootService.game)
        val stack = drawStack

        // clear current visual stack
        stack.clear()

        // i deleted this check, as in GameService I ensured that draw stack is never empty when refreshing it
        // if draw stack is empty do nothing
        // if(g.drawStack.isEmpty()) return

        // if not empty, show back of all cards in the stack
        for (card in g.drawStack) {
            val view = cardViews[card]
            view.showBack()
            stack.add(view)
        }
    }

    // Refresh score labels
    override fun refreshScore() {
        val g = requireNotNull(rootService.game)
        val player = g.currentPlayer

        scoreLabel.text = "Score: ${player.score}"
    }

    // Refresh log label to show latest messages
    override fun refreshLog() {
        val g = requireNotNull(rootService.game)

        // nothing new to show
        if (lastLogIndex >= g.log.size) return

        // build only the new part
        val sb = StringBuilder()

        for (i in lastLogIndex until g.log.size) {
            sb.append("- ").append(g.log[i]).append("\n")
        }

        // append new lines to what is already displayed
        logArea.text += sb.toString()

        // update where we stopped
        lastLogIndex = g.log.size
    }

    // Refresh after turn ends of the current player
    override fun refreshAfterTurn() {
        val g = requireNotNull(rootService.game)

        Timer().schedule(1000) {
            app.showStartTurn()
            playerNameLabel.text = "Player: ${g.currentPlayer.name}"
        }

    }

    // Refresh to end game scene
    override fun refreshToEndGame() {
        Timer().schedule(1000) {
            app.showResult()
        }
    }


    // helper functions to highlight selected cards
    private fun highlight(view: CardView) {
        view.scale = 1.10          // slightly bigger
        view.opacity = 0.85        // a bit transparent
        view.rotation = 2.0

    }

    // helper function to unhighlight cards
    private fun unhighlight(view: CardView) {
        view.scale = 1.0
        view.opacity = 1.0
        view.rotation = 0.0
    }

    private fun clearSelections() {
        val handView = selectedHandView
        if (handView != null) {
            unhighlight(handView)
        }

        val stairView = selectedStairView
        if (stairView != null) {
            unhighlight(stairView)
        }

        selectedHandView = null
        selectedStairView = null
        selectedHandCard = null
        selectedStairCard = null
    }
}