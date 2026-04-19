package entity

/**
 * to save a snapshot of the game result at the end of the game before putting game to null
 * */
data class GameResult(
    val player1Name: String,
    val player2Name: String,
    val player1Score: Int,
    val player2Score: Int,
    val player1Collected: List<Card>,
    val player2Collected: List<Card>,
    val winnerName: String // or "it's a tie"
)