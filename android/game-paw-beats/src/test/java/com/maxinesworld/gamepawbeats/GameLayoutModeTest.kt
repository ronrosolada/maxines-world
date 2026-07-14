package com.maxinesworld.gamepawbeats

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class GameLayoutModeTest {
    @Test fun `short landscape is phone even when width exceeds 600`() {
        assertEquals(GameLayoutMode.PhoneLandscape, gameLayoutMode(640.dp, 360.dp))
    }

    @Test fun `compact portrait is phone`() {
        assertEquals(GameLayoutMode.PhonePortrait, gameLayoutMode(393.dp, 873.dp))
    }

    @Test fun `600dp portrait is tablet medium`() {
        assertEquals(GameLayoutMode.TabletMedium, gameLayoutMode(600.dp, 960.dp))
    }

    @Test fun `expanded tablet requires sufficient width and height`() {
        assertEquals(GameLayoutMode.TabletExpanded, gameLayoutMode(840.dp, 1180.dp))
        assertEquals(GameLayoutMode.TabletMedium, gameLayoutMode(840.dp, 580.dp))
    }
}
