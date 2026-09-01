package com.maxinesworld.app

/**
 * Kid-first shelf curation for the reward-break mini-game library.
 *
 * The full catalog remains bundled for offline assets, but the child shelf is
 * an allowlist of games that fit an 8-year-old Grade 3 reward break. Language-
 * heavy and adult strategy games stay off the shelf and cannot be opened
 * through the child route.
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

    /** Extra simple classics that still fit an 8-year-old break. */
    internal val additionalChildGames: List<String> = listOf(
        "tictactoe",
        "connect-four",
        "sliding-puzzle",
        "word-search",
        "tetris",
        "pong",
        "2048",
        "onet-connect",
    )

    /** Adult strategy, card, dice, and hard word games hidden from children. */
    internal val hiddenFromChildShelf: Set<String> = setOf(
        "wordle",
        "sudoku",
        "solitaire",
        "freecell",
        "checkers",
        "reversi",
        "mancala",
        "yahtzee",
        "domino",
    )

    internal val childAllowlist: List<String> = kidFriendlyFirstOrder + additionalChildGames

    fun isChildFacing(slug: String): Boolean = slug in childAllowlist

    /**
     * Returns only allowlisted games, kid-friendly first, then remaining
     * allowlisted titles in the fixed extra order. Hidden games never appear.
     */
    fun shelfOrder(games: List<EmbeddedMiniGame>): List<EmbeddedMiniGame> {
        val bySlug = games.associateBy { it.slug }
        return childAllowlist.mapNotNull { slug -> bySlug[slug] }
    }

    /** The first shelf items a child sees; used by the library header copy. */
    fun kidFriendlyCount(games: List<EmbeddedMiniGame>): Int =
        games.count { it.slug in kidFriendlyFirstOrder }
}
