package service

import entity.*
/**
 * The root service class is responsible for managing services and the entity layer reference.
 * This class acts as a central hub for every other service within the application.
 *
 */
class RootService : AbstractRefreshingService() {

    /**Save inside game the current running game attributes or null if no game is running*/
    var game : KartentreppeGame? = null

    /**
     * last game snapshot for ResultScene
     * */
    var lastResult: GameResult? = null

    /**Using Internal Set so the game scenes from view package can't make changes they just receive
     * the new scene*/
    internal set



    /**I store all gameService services inside this variable */
    var gameService = GameService(this)


    /**I store all playerService services inside this variable */
    var playerService = PlayerService(this)

    /**
     * helper for GameService to store result, in endGame() method
     * */
    internal fun storeResult(result: GameResult) {
        lastResult = result
    }

}