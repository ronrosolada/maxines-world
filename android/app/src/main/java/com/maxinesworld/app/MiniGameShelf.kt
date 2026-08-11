package com.maxinesworld.app

/**
 * Kid-first shelf curation for the reward-break mini-game library.
 *
 * The full 29-game catalog stays available for offline play, but the shelf
 * shows the games that are most likely to delight an 8-year-old first, and
 * keeps language-heavy or strategy-heavy classics behind the fold so the
 * default view never looks like an adult puzzle collection.
 */
object MiniGameShelf {

    /** Slugs deliberately surfaced first on the child-facing shelf. */
    internal val kidFriendlyFirstOrder: List<String> = listOf(
        "memory-match",
        "whack-a-mole",
        "match-three",
        "piano-tiles",
        "snake",
        "stack",
        "breakout",
        "flappy-bird",
        "color-connect",
        "bolt-sort",
        "number-merge",
        "color-block",
    )

    /** Slugs that are heavy on text or adult strategy; shown last. */
    internal val languageHeavyLastOrder: List<String> = listOf(
        "wordle",
        "word-search",
        "solitaire",
        "freecell",
        "sudoku",
        "checkers",
        "reversi",
        "mancala",
        "domino",
        "yahtzee",
        "connect-four",
        "tictactoe",
        "pong",
        "tetris",
        "sliding-puzzle",
        "onet-connect",
        "2048",
    )

    /**
     * Returns the full catalog ordered for the child shelf: kid-friendly
     * games first (in a fixed order), then the remaining classics in their
     * original catalog order. Every catalog game is present exactly once.
     */
    fun shelfOrder(games: List<EmbeddedMiniGame>): List<EmbeddedMiniGame> {
        val bySlug = games.associateBy { it.slug }
        val ordered = buildList {
            kidFriendlyFirstOrder.forEach { slug ->
                bySlug[slug]?.let(::add)
            }
            games.forEach { game ->
                if (game.slug !in kidFriendlyFirstOrder) add(game)
            }
        }
        return ordered
    }

    /** The first shelf items a child sees; used by the library header copy. */
    fun kidFriendlyCount(games: List<EmbeddedMiniGame>): Int =
        games.count { it.slug in kidFriendlyFirstOrder }
}
