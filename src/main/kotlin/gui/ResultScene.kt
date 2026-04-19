package gui

import entity.*
import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.gamecomponentviews.CardView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.components.ComponentView
import kotlin.system.exitProcess

/**
 * Here is the scene where the players see the final Result and who won the game
 * */
class ResultScene(val app: SopraApplication, val rootService: RootService):
        BoardGameScene(1920, 1080), Refreshable {

    // So I can create views of cards
    private val imageLoader = CardImageLoader()

    // to use the dimensions directly
    private val cardWidth = 85.0
    private val cardHeight = 130.0

    // Adjusting layout dimensions
    private val pairGap = 10.0
    private val colGap = 25.0  //gap between columns horizontally
    private val rowGap = 20.0  //gap between rows vertically
    private val maxPairPerRow = 4

    // Pane to hold the results
    private val backgroundPane = Pane<ComponentView> (
        posX = 0,
        posY = 0,
        width = 1920,
        height = 1080,
        visual = ColorVisual(0, 128, 0)  // Green background
    )


    //Divider in the middle
    private val divider = Pane<ComponentView> (
        posX = 960,
        posY = 60,
        width = 4,
        height = 900,
        visual = ColorVisual(200, 200, 200)  // Light gray divider
    )

    //Left half
    private val leftHalf = Pane<ComponentView> (
        posX = 0,
        posY = 0,
        width = 960,
        height = 1080,
    )

    //Right half
    private val rightHalf = Pane<ComponentView> (
        posX = 960,
        posY = 0,
        width = 960,
        height = 1080,
    )

    //Player1 Header
    private val p1NameLabel = Label(
        posX = 180,
        posY = 70,
        width = 600,
        height = 60,
        text = "",
        alignment = Alignment.CENTER
    ).apply {
        font = Font(size = 26, color = Color.BLACK)
    }

    //Player2 Header same as above as I will add it to right half
    private val p2NameLabel = Label(
        posX = 180,
        posY = 70,
        width = 600,
        height = 60,
        text = "",
        alignment = Alignment.CENTER
    ).apply {
        font = Font(size = 26, color = Color.BLACK)
    }

    //score label for player 1
    private val p1ScoreLabel = Label(
        posX = 330,
        posY = 150,
        width = 300,
        height = 45,
        text = "",
        alignment = Alignment.CENTER,
        visual = ColorVisual(230, 230, 230)
    ).apply {
        font = Font(size = 20, color = Color.BLACK)
    }

    //score label for player 2 same as above as I will add it to right half
    private val p2ScoreLabel = Label(
        posX = 330,
        posY = 150,
        width = 300,
        height = 45,
        text = "",
        alignment = Alignment.CENTER,
        visual = ColorVisual(230, 230, 230)
    ).apply {
        font = Font(size = 20, color = Color.BLACK)
    }


    // Collected area for player 1
    private val p1CollectedArea = Pane<ComponentView>(
        posX = 80.0,
        posY = 230.0,
        width = 800.0,
        height = 650.0
    )

    // Collected area for player 2
    private val p2CollectedArea = Pane<ComponentView>(
        posX = 80.0,
        posY = 230.0,
        width = 800.0,
        height = 650.0
    )

    // Restart Button
    val restartButton = Button(
        posX = 730,
        posY = 980,
        text = "Restart",
        visual = ColorVisual(230, 230, 230)
    ).apply {
        font = Font(size = 18, color = Color.BLACK)
    }

    // exit Button
    private val exitButton = Button(
        posX = 1060,           // a bit to the right of restartButton (860)
        posY = 980,
        text = "Exit",
        visual = ColorVisual(230, 230, 230)
    ).apply {
        font = Font(size = 18, color = Color.BLACK)
    }


    // Initialization block
    init {
        background = ColorVisual(0, 128, 0) // Green background

        leftHalf.add(p1NameLabel)
        leftHalf.add(p1ScoreLabel)
        leftHalf.add(p1CollectedArea)

        rightHalf.add(p2NameLabel)
        rightHalf.add(p2ScoreLabel)
        rightHalf.add(p2CollectedArea)

        backgroundPane.add(leftHalf)
        backgroundPane.add(rightHalf)
        backgroundPane.add(divider)

        addComponents(backgroundPane, restartButton, exitButton)

        restartButton.onMouseClicked = {
            app.showMainMenu()
            clearResultUI()
        }

        exitButton.onMouseClicked = {
            clearResultUI()
            app.exit()
            exitProcess(0)
        }
    }

    // implement Refreshable
    override fun refreshToEndGame() {
        val result = requireNotNull(rootService.lastResult)

        p1NameLabel.text = result.player1Name
        p2NameLabel.text = result.player2Name

        p1ScoreLabel.text = "Score: ${result.player1Score}"
        p2ScoreLabel.text = "Score: ${result.player2Score}"

        // highlight winner
        if (result.winnerName == result.player1Name) {
            p1NameLabel.visual = ColorVisual(255, 255, 150) // yellow
        } else if (result.winnerName == result.player2Name) {
            p2NameLabel.visual = ColorVisual(255, 255, 150)
        }

        // show collected cards (pairs)
        showPairs(result.player1Collected, p1CollectedArea)
        showPairs(result.player2Collected, p2CollectedArea)
    }


    // helper function to show pairs of collected cards in given area
    private fun showPairs(cards: List<Card>, area: Pane<ComponentView>) {
        // clear previous views if any
        // although i cleared it in clearResultUI, but just to be safe
        area.clear()

        // position of the next pair
        var x = 0.0
        var y = 0.0
        var pairsInRow = 0

        var i = 0
        while (i < cards.size) {
            val c1 = cards[i]
            val c2 = cards[i + 1] // the pair card

            val v1 = CardView(
                posX = x,
                posY = y,
                width = cardWidth,
                height = cardHeight,
                front = imageLoader.frontImageFor(c1.suit, c1.value),
                back = imageLoader.backImage
            ).apply {
                showFront()
            }

            val v2 = CardView(
                posX = x + cardWidth + pairGap,
                posY = y,
                width = cardWidth,
                height = cardHeight,
                front = imageLoader.frontImageFor(c2.suit, c2.value),
                back = imageLoader.backImage
            ).apply {
                showFront()
            }

            area.add(v1)
            area.add(v2)

            x += (2 * cardWidth) + pairGap + colGap
            pairsInRow++

            if(pairsInRow == maxPairPerRow) {
                // move to next row
                x = 0.0
                y += cardHeight + rowGap
                pairsInRow = 0
            }

            i += 2 // move to next pair

        }


    }




    // clear the result UI for next game
    private fun clearResultUI() {
        p1NameLabel.text = ""
        p2NameLabel.text = ""
        p1ScoreLabel.text = ""
        p2ScoreLabel.text = ""

        p1NameLabel.visual = ColorVisual(230, 230, 230)
        p2NameLabel.visual = ColorVisual(230, 230, 230)

        p1CollectedArea.clear()
        p2CollectedArea.clear()
    }





}