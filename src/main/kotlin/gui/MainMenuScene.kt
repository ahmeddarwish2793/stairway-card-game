package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.UIComponent
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.Alignment
import kotlin.system.exitProcess

/**
 * In this scene I want to implement :
 * 2 text fields for player names
 * a button called start button which call gameScene by clicking on it*/
class MainMenuScene(val app: SopraApplication, val rootService: RootService): BoardGameScene(), Refreshable {

    // Label to show the title of the game
    private val titleLabel = Label(
        width = 900,
        height = 70,
        posX = 500,
        posY = 120,
        text = "Welcome to Kartentreppe Game",
        alignment = Alignment.CENTER,
        font = Font(size = 40)
    )

    // Player 1 label beside text field
    private val player1Label = Label(
        width = 160.0,
        height = 40.0,
        posX = 600.0,
        posY = 260.0,
        text = "Player 1:",
        alignment = Alignment.CENTER_RIGHT
    ).apply {
        font = Font(size = 22, color = Color.BLACK)
    }

    // Player 2 label beside text field
    private val player2Label = Label(
        width = 160.0,
        height = 40.0,
        posX = 600.0,
        posY = 370.0,
        text = "Player 2:",
        alignment = Alignment.CENTER_RIGHT
    ).apply {
        font = Font(size = 22, color = Color.BLACK)
    }

    // Text field for player 1 name
    val player1TextField = TextField(
        width = 420,
        height = 40,
        posX = 780,
        posY = 260,
        text = ""
    ).apply {
        font = Font(size = 20)
        visual = ColorVisual(Color.WHITE)
    }

    // Text field for player 2 name
    val player2TextField = TextField(
        width = 420,
        height = 40,
        posX = 780,
        posY = 370,
        text = ""
    ).apply {
        font = Font(size = 20)
        visual = ColorVisual(Color.WHITE)
    }

    // Start button to begin the game
    private val startButton = Button(
        width = 200,
        height = 50,
        posX = 860,
        posY = 480,
        text = "Start",
        font = Font(size = 24),
        visual = ColorVisual(color = Color(0xFFFFFF))
    ).apply {
        onMouseClicked = {
            try {
                // try to start game with given player names
                rootService.gameService.startGame(
                    player1TextField.text,
                    player2TextField.text
                )

                // we will reach here only if no exception is thrown
                // show the game scene
                app.showGame()

            } catch (e: IllegalArgumentException) {
                // show the message from GameService
                showError(e.message ?: "Invalid input. Please try again.")
            }
        }
    }

    // Exit button to close the application
    private val exitButton = Button(
        width = 200,
        height = 50,
        posX = 860,
        posY = 550,              // under start button
        text = "Exit",
        font = Font(size = 24),
        visual = ColorVisual(color = Color(0xFFFFFF))
    ).apply {
        onMouseClicked = {
            // Exit the application
            app.exit()
            exitProcess(0)
        }
    }


    //pop-up window to show error if player names are invalid
    private val errorPopup = Pane<UIComponent>(
        posX = 560,
        posY = 200,
        width = 800,
        height = 250,
        visual = ColorVisual(0, 0, 0, 180) // Semi-transparent black background
    ).apply { isVisible = false }

    private val errorLabel = Label(
        posX = 40,
        posY = 40,
        width = 720,
        height = 120,
        text = "",
        alignment = Alignment.CENTER
    ).apply {
        font = Font(size = 22, color = Color.WHITE)
    }

    private val errorOkButton = Button(
        posX = 300,
        posY = 170,
        width = 200,
        height = 50,
        text = "OK",
        visual = ColorVisual(230, 230, 230)
    ).apply {
        font = Font(size = 20, color = Color.BLACK)
        onMouseClicked = {
            errorPopup.isVisible = false
        }
    }

    init {
        background = ColorVisual(0, 128, 0) // Green background

        errorPopup.add(errorLabel)
        errorPopup.add(errorOkButton)

        addComponents(titleLabel, player1TextField, player2TextField,
            player1Label, player2Label,
            startButton, exitButton,
            errorPopup)
    }

    /**
     * Here I am going to implement only refreshAfterStartGame since this is the only relevant
     * refresh for main menu scene
     * and I will write the other refreshers to avoid CI push error as last time */

    override fun refreshAfterStartGame() {
        // Clear text fields after starting the game
        player1TextField.text = ""
        player2TextField.text = ""
    }

    // helper function to show error popup with message
    private fun showError(message: String) {
        errorLabel.text = message
        errorPopup.isVisible = true
    }

}