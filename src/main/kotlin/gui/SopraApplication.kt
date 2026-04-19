package gui

import tools.aqua.bgw.core.BoardGameApplication
import service.RootService

/**
 * Represents the main application for the SoPra board game.
 * The application initializes the [RootService] and displays the scenes.
 */
class SopraApplication : BoardGameApplication("SoPra Game") {

    /**
     * The root service instance. This is used to call service methods and access the entity layer.
     */
    val rootService: RootService = RootService()

    /**
     * The main game scene displayed in the application.
     * I replaced the HelloScene with the mainMenuScene as the starting scene.
     * here are the variables for the different scenes
     */
    private val mainMenuScene = MainMenuScene(this, rootService)
    val gameScene = GameScene(this, rootService)
    val startTurnScene = StartTurnScene(this, rootService)
    val resultScene = ResultScene(this, rootService)

    /**
     * Initializes the application.
     */
    init {
        //I want to add scenes to refreshables so they can be notified when to refresh
        rootService.gameService.addRefreshable(mainMenuScene)
        rootService.gameService.addRefreshable(gameScene)
        rootService.gameService.addRefreshable(startTurnScene)
        rootService.gameService.addRefreshable(resultScene)

        rootService.addRefreshable(mainMenuScene)
        rootService.addRefreshable(gameScene)
        rootService.addRefreshable(startTurnScene)
        rootService.addRefreshable(resultScene)

        rootService.playerService.addRefreshable(mainMenuScene)
        rootService.playerService.addRefreshable(gameScene)
        rootService.playerService.addRefreshable(startTurnScene)
        rootService.playerService.addRefreshable(resultScene)


        showMainMenu()
    }

    /**
     * function to call main menu scene
     * */
    fun showMainMenu() = showGameScene(mainMenuScene)

    /**
     * function to call game scene
     * */
    fun showGame() = showGameScene(gameScene)

    /**
     * function to call start turn scene
     * */
    fun showStartTurn() = showGameScene(startTurnScene)

    /**
     * function to call result scene
     * */
    fun showResult() = showGameScene(resultScene)
}



