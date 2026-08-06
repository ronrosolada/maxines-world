package com.maxinesworld.app

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
    val icon: String,
) {
    val gameId: String get() = "source-mini-$slug"
}

object MiniGameCatalog {
    const val SOURCE_COMMIT = "a9421318e6f4644c5f144df78576114db60de8a6"
    const val ASSET_ROOT = "mini-games/games"

    val games: List<EmbeddedMiniGame> = listOf(
        EmbeddedMiniGame("2048", "2048", "Slide and merge number tiles to reach the target.", EmbeddedMiniGameCategory.PUZZLE, "🔢"),
        EmbeddedMiniGame("bolt-sort", "Bolt Sort", "Sort each color into its own bolt.", EmbeddedMiniGameCategory.PUZZLE, "🔩"),
        EmbeddedMiniGame("breakout", "Breakout", "Bounce the ball and clear the colorful bricks.", EmbeddedMiniGameCategory.ARCADE, "🧱"),
        EmbeddedMiniGame("checkers", "Checkers", "Plan your jumps and capture the other pieces.", EmbeddedMiniGameCategory.BOARD, "🔴"),
        EmbeddedMiniGame("color-block", "Color Block", "Place shapes and clear full rows and columns.", EmbeddedMiniGameCategory.PUZZLE, "🟦"),
        EmbeddedMiniGame("color-connect", "Color Connect", "Connect matching colors without crossing paths.", EmbeddedMiniGameCategory.PUZZLE, "🔗"),
        EmbeddedMiniGame("connect-four", "Connect Four", "Drop discs and connect four in a row.", EmbeddedMiniGameCategory.BOARD, "🟡"),
        EmbeddedMiniGame("domino", "Domino", "Match tiles end-to-end and build a chain.", EmbeddedMiniGameCategory.BOARD, "🎲"),
        EmbeddedMiniGame("flappy-bird", "Flappy Bird", "Tap to guide the bird through the open spaces.", EmbeddedMiniGameCategory.ARCADE, "🐦"),
        EmbeddedMiniGame("freecell", "FreeCell", "Plan card moves and build the four foundation piles.", EmbeddedMiniGameCategory.BOARD, "🂠"),
        EmbeddedMiniGame("mancala", "Mancala", "Sow seeds around the board and collect the most.", EmbeddedMiniGameCategory.BOARD, "🪨"),
        EmbeddedMiniGame("match-three", "Match Three", "Swap tiles to make lines of three or more.", EmbeddedMiniGameCategory.PUZZLE, "🍬"),
        EmbeddedMiniGame("memory-match", "Memory Match", "Find the matching pairs and exercise your memory.", EmbeddedMiniGameCategory.BOARD, "🧠"),
        EmbeddedMiniGame("number-merge", "Number Merge", "Drop equal neighbors together to combine them.", EmbeddedMiniGameCategory.PUZZLE, "🔀"),
        EmbeddedMiniGame("onet-connect", "Onet Connect", "Connect matching tiles with no more than two turns.", EmbeddedMiniGameCategory.PUZZLE, "🀄"),
        EmbeddedMiniGame("piano-tiles", "Piano Tiles", "Tap the falling tiles before they pass.", EmbeddedMiniGameCategory.ARCADE, "🎹"),
        EmbeddedMiniGame("pong", "Pong", "Keep the ball moving and score against the paddle.", EmbeddedMiniGameCategory.ARCADE, "🏓"),
        EmbeddedMiniGame("reversi", "Reversi", "Outflank pieces and finish with the most discs.", EmbeddedMiniGameCategory.BOARD, "⚫"),
        EmbeddedMiniGame("sliding-puzzle", "Sliding Puzzle", "Slide the tiles into their correct order.", EmbeddedMiniGameCategory.PUZZLE, "🔲"),
        EmbeddedMiniGame("snake", "Snake", "Guide the snake to food without hitting the edges.", EmbeddedMiniGameCategory.ARCADE, "🐍"),
        EmbeddedMiniGame("solitaire", "Solitaire", "Build the four foundation piles from Ace to King.", EmbeddedMiniGameCategory.BOARD, "♠️"),
        EmbeddedMiniGame("stack", "Stack", "Time each drop to build the tallest tower.", EmbeddedMiniGameCategory.ARCADE, "📦"),
        EmbeddedMiniGame("sudoku", "Sudoku", "Use logic to fill every row, column, and box.", EmbeddedMiniGameCategory.PUZZLE, "🧩"),
        EmbeddedMiniGame("tetris", "Tetris", "Fit falling pieces together and clear lines.", EmbeddedMiniGameCategory.ARCADE, "🟥"),
        EmbeddedMiniGame("tictactoe", "Tic-Tac-Toe", "Make a line of three before your opponent does.", EmbeddedMiniGameCategory.BOARD, "⭕"),
        EmbeddedMiniGame("whack-a-mole", "Whack-a-Mole", "Tap the friendly hedgehogs before they hide.", EmbeddedMiniGameCategory.ARCADE, "🦔"),
        EmbeddedMiniGame("word-search", "Word Search", "Find the hidden words in the letter grid.", EmbeddedMiniGameCategory.WORD, "🔤"),
        EmbeddedMiniGame("wordle", "Wordle", "Use clues to discover the hidden word.", EmbeddedMiniGameCategory.WORD, "🟩"),
        EmbeddedMiniGame("yahtzee", "Yahtzee", "Roll, choose, and make the best dice combinations.", EmbeddedMiniGameCategory.BOARD, "🎯"),
    )

    private val bySlug = games.associateBy(EmbeddedMiniGame::slug)

    fun find(slug: String): EmbeddedMiniGame? = bySlug[slug]
}
