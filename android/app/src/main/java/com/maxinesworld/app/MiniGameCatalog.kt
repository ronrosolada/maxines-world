package com.maxinesworld.app

import androidx.annotation.DrawableRes

/**
 * Offline games imported from the pinned upstream mini-games source.
 *
 * This catalog is deliberately curated for Maxine's child-safe reward breaks;
 * it is not a general-purpose browser or external-content launcher.
 */
enum class EmbeddedMiniGameCategory(val label: String) {
    ARCADE("Arcade"),
    PUZZLE("Puzzle"),
    BOARD("Board"),
    WORD("Word"),
}

data class EmbeddedMiniGame(
    val slug: String,
    val title: String,
    val description: String,
    val category: EmbeddedMiniGameCategory,
    @DrawableRes val thumbnailRes: Int,
) {
    val gameId: String get() = "source-mini-$slug"
}

object MiniGameCatalog {
    const val SOURCE_COMMIT = "a9421318e6f4644c5f144df78576114db60de8a6"
    const val ASSET_ROOT = "mini-games/games"

    val games: List<EmbeddedMiniGame> = listOf(
        EmbeddedMiniGame("2048", "2048", "Slide and merge number tiles to reach the target.", EmbeddedMiniGameCategory.PUZZLE, R.drawable.mw_game_2048),
        EmbeddedMiniGame("bolt-sort", "Bolt Sort", "Sort each color into its own bolt.", EmbeddedMiniGameCategory.PUZZLE, R.drawable.mw_game_bolt_sort),
        EmbeddedMiniGame("breakout", "Breakout", "Bounce the ball and clear the colorful bricks.", EmbeddedMiniGameCategory.ARCADE, R.drawable.mw_game_breakout),
        EmbeddedMiniGame("checkers", "Checkers", "Plan your jumps and capture the other pieces.", EmbeddedMiniGameCategory.BOARD, R.drawable.mw_game_checkers),
        EmbeddedMiniGame("color-block", "Color Block", "Place shapes and clear full rows and columns.", EmbeddedMiniGameCategory.PUZZLE, R.drawable.mw_game_color_block),
        EmbeddedMiniGame("color-connect", "Color Connect", "Connect matching colors without crossing paths.", EmbeddedMiniGameCategory.PUZZLE, R.drawable.mw_game_color_connect),
        EmbeddedMiniGame("connect-four", "Connect Four", "Drop discs and connect four in a row.", EmbeddedMiniGameCategory.BOARD, R.drawable.mw_game_connect_four),
        EmbeddedMiniGame("domino", "Domino", "Match tiles end-to-end and build a chain.", EmbeddedMiniGameCategory.BOARD, R.drawable.mw_game_domino),
        EmbeddedMiniGame("flappy-bird", "Flappy Bird", "Tap to guide the bird through the open spaces.", EmbeddedMiniGameCategory.ARCADE, R.drawable.mw_game_flappy_bird),
        EmbeddedMiniGame("freecell", "FreeCell", "Plan card moves and build the four foundation piles.", EmbeddedMiniGameCategory.BOARD, R.drawable.mw_game_freecell),
        EmbeddedMiniGame("mancala", "Mancala", "Sow seeds around the board and collect the most.", EmbeddedMiniGameCategory.BOARD, R.drawable.mw_game_mancala),
        EmbeddedMiniGame("match-three", "Match Three", "Swap tiles to make lines of three or more.", EmbeddedMiniGameCategory.PUZZLE, R.drawable.mw_game_match_three),
        EmbeddedMiniGame("memory-match", "Memory Match", "Find the matching pairs and exercise your memory.", EmbeddedMiniGameCategory.BOARD, R.drawable.mw_game_memory_match),
        EmbeddedMiniGame("number-merge", "Number Merge", "Drop equal neighbors together to combine them.", EmbeddedMiniGameCategory.PUZZLE, R.drawable.mw_game_number_merge),
        EmbeddedMiniGame("onet-connect", "Onet Connect", "Connect matching tiles with no more than two turns.", EmbeddedMiniGameCategory.PUZZLE, R.drawable.mw_game_onet_connect),
        EmbeddedMiniGame("piano-tiles", "Piano Tiles", "Tap the falling tiles before they pass.", EmbeddedMiniGameCategory.ARCADE, R.drawable.mw_game_piano_tiles),
        EmbeddedMiniGame("pong", "Pong", "Keep the ball moving and score against the paddle.", EmbeddedMiniGameCategory.ARCADE, R.drawable.mw_game_pong),
        EmbeddedMiniGame("reversi", "Reversi", "Outflank pieces and finish with the most discs.", EmbeddedMiniGameCategory.BOARD, R.drawable.mw_game_reversi),
        EmbeddedMiniGame("sliding-puzzle", "Sliding Puzzle", "Slide the tiles into their correct order.", EmbeddedMiniGameCategory.PUZZLE, R.drawable.mw_game_sliding_puzzle),
        EmbeddedMiniGame("snake", "Snake", "Guide the snake to food without hitting the edges.", EmbeddedMiniGameCategory.ARCADE, R.drawable.mw_game_snake),
        EmbeddedMiniGame("solitaire", "Solitaire", "Build the four foundation piles from Ace to King.", EmbeddedMiniGameCategory.BOARD, R.drawable.mw_game_solitaire),
        EmbeddedMiniGame("stack", "Stack", "Time each drop to build the tallest tower.", EmbeddedMiniGameCategory.ARCADE, R.drawable.mw_game_stack),
        EmbeddedMiniGame("sudoku", "Sudoku", "Use logic to fill every row, column, and box.", EmbeddedMiniGameCategory.PUZZLE, R.drawable.mw_game_sudoku),
        EmbeddedMiniGame("tetris", "Tetris", "Fit falling pieces together and clear lines.", EmbeddedMiniGameCategory.ARCADE, R.drawable.mw_game_tetris),
        EmbeddedMiniGame("tictactoe", "Tic-Tac-Toe", "Make a line of three before your opponent does.", EmbeddedMiniGameCategory.BOARD, R.drawable.mw_game_tictactoe),
        EmbeddedMiniGame("whack-a-mole", "Whack-a-Mole", "Tap the friendly hedgehogs before they hide.", EmbeddedMiniGameCategory.ARCADE, R.drawable.mw_game_whack_a_mole),
        EmbeddedMiniGame("word-search", "Word Search", "Find the hidden words in the letter grid.", EmbeddedMiniGameCategory.WORD, R.drawable.mw_game_word_search),
        EmbeddedMiniGame("wordle", "Wordle", "Use clues to discover the hidden word.", EmbeddedMiniGameCategory.WORD, R.drawable.mw_game_wordle),
        EmbeddedMiniGame("yahtzee", "Yahtzee", "Roll, choose, and make the best dice combinations.", EmbeddedMiniGameCategory.BOARD, R.drawable.mw_game_yahtzee),
    )

    private val bySlug = games.associateBy(EmbeddedMiniGame::slug)

    fun find(slug: String): EmbeddedMiniGame? = bySlug[slug]
}
