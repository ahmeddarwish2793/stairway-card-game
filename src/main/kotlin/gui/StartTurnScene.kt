package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.components.uicomponents.UIComponent
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.core.Color

/**
 * This scene is shown at the beginning of each player's turn
 * It displays the name of the player whose turn it is
 * and has a button to start the turn
 * */
class StartTurnScene(val app: SopraApplication, val rootService: RootService):
    BoardGameScene(1920, 1080), Refreshable {

    //The overlay pane to darken the background
    private val overlay = Pane<Label>(
        posX = 0,
        posY = 0,
        width = 1920,
        height = 1080,
        visual = ColorVisual(0, 0, 0, 120) // Semi-transparent black
    )

    // popup label
    private val popup = Pane<UIComponent>(
        posX = 650,
        posY = 350,
        width = 620,
        height = 320,
        visual = ColorVisual(25, 25, 25) // Dark background for the popup
    )

    private val titleLabel = Label(
        posX = 0,
        posY = 40,
        width = 620,
        height = 60,
        text = "Next Player's Turn",
        alignment = Alignment.CENTER
    ).apply {
        font = Font(size = 22, color = Color(230, 230, 230))
    }

    private val playerLabel = Label(
        posX = 110,
        posY = 120,
        width = 400,
        height = 60,
        text = "",
        alignment = Alignment.CENTER,
        visual = ColorVisual(80, 80, 80)
    )

    private val startButton = Button(
        posX = 210,
        posY = 210,
        width = 200,
        height = 60,
        text = "Start my turn"
    )

    init {
        addComponents(overlay, popup)
        popup.add(titleLabel)
        popup.add(playerLabel)
        popup.add(startButton)

        startButton.onMouseClicked = {
            // show GameScene again
            app.showGame()

            // inline full refresh for NEW current player
            rootService.onAllRefreshables {
                refreshHand(-2)        // show current player's hand, hide opponent
                refreshScore()
                refreshLog()
            }
        }
    }

    // called from PlayerService.startTurn()
    override fun refreshAfterTurn() {
        val g = requireNotNull(rootService.game)
        playerLabel.text = g.currentPlayer.name
    }
}