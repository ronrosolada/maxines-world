package com.maxinesworld.engineactivity

import com.maxinesworld.engineactivity.renderers.svgAssetPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssetSvgPreviewTest {

    @Test
    fun `asset id resolves to the bundled vector path`() {
        assertEquals(
            "content-pack/month-01/assets/vectors/english-g3-q1-w01-d01-visual.svg",
            svgAssetPath("english-g3-q1-w01-d01-visual")
        )
    }

    @Test
    fun `asset path rejects traversal and separators`() {
        assertNull(svgAssetPath("../secret"))
        assertNull(svgAssetPath("nested/asset"))
        assertNull(svgAssetPath(""))
    }
}