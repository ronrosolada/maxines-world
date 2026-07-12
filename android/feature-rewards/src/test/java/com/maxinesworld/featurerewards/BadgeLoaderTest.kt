package com.maxinesworld.featurerewards

import android.content.Context
import android.content.res.AssetManager
import com.maxinesworld.coremodel.CollectibleBadge
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class BadgeLoaderTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ─────────────────────────────────────────────
    // TEST: loadAll parses all 50 badges across 5 biomes
    // ─────────────────────────────────────────────
    @Test
    fun `loadAll parses badge_catalog json with 50 entries across 5 biomes`() = runTest {
        val testJson = buildBadgeCatalogJson()
        val (loader, _) = createLoaderWithJson(testJson)

        val badges = loader.loadAll()

        assertEquals("total badges", 50, badges.size)

        // Verify 5 biomes each with exactly 10 badges
        val biomes = badges.groupBy { it.biome }
        assertEquals("5 biomes", 5, biomes.size)
        biomes.forEach { (biome, list) ->
            assertEquals("biome $biome has 10 badges", 10, list.size)
        }

        // Spot-check specific entries
        val forestBadge = badges.first { it.id == "mammal_tarsier" }
        assertEquals("tarsier biome", "forest_friends", forestBadge.biome)
        assertEquals("tarsier name", "Philippine tarsier", forestBadge.name)
        assertEquals("tarsier title", "Moon-Eyed Jumper", forestBadge.title)
        assertTrue("tarsier funFact non-empty", forestBadge.funFact.isNotBlank())
        assertEquals("tarsier emoji", "🐒", forestBadge.emoji)
        assertFalse("not collected by default", forestBadge.isCollected)

        val eagleBadge = badges.first { it.id == "bird_eagle" }
        assertEquals("eagle biome", "sky_scouts", eagleBadge.biome)
        assertEquals("eagle name", "Philippine eagle", eagleBadge.name)

        val crocBadge = badges.first { it.id == "reptile_philippine_crocodile" }
        assertEquals("croc biome", "river_guardians", crocBadge.biome)

        val frogBadge = badges.first { it.id == "amphibian_mindoro_tree_frog" }
        assertEquals("frog biome", "creek_coral", frogBadge.biome)

        val birdBadge = badges.first { it.id == "bird_cebu_flowerpecker" }
        assertEquals("flowerpecker biome", "songbird_grove", birdBadge.biome)
    }

    // ─────────────────────────────────────────────
    // TEST: loadAll caches result (2nd call uses cache)
    // ─────────────────────────────────────────────
    @Test
    fun `loadAll caches result and does not re-read assets on second call`() = runTest {
        val testJson = buildBadgeCatalogJson()
        val (loader, mockAssets) = createLoaderWithJson(testJson)

        // First call
        val first = loader.loadAll()
        assertEquals(50, first.size)

        // Second call — should use cache, not re-open assets
        val second = loader.loadAll()
        assertEquals(50, second.size)
        assertSame("cached instance returned", first, second)

        // Assets should have been opened exactly once
        verify(exactly = 1) { mockAssets.open("badge_catalog.json") }
    }

    // ─────────────────────────────────────────────
    // TEST: all badge IDs follow naming convention
    // ─────────────────────────────────────────────
    @Test
    fun `all badge IDs are unique and follow expected prefixes`() = runTest {
        val testJson = buildBadgeCatalogJson()
        val (loader, _) = createLoaderWithJson(testJson)

        val badges = loader.loadAll()
        val ids = badges.map { it.id }

        // No duplicates
        assertEquals("all IDs unique", ids.size, ids.toSet().size)

        // Count by prefix
        val mammalCount = ids.count { it.startsWith("mammal_") }
        val birdCount = ids.count { it.startsWith("bird_") }
        val reptileCount = ids.count { it.startsWith("reptile_") }
        val amphibianCount = ids.count { it.startsWith("amphibian_") }
        val fishCount = ids.count { it.startsWith("fish_") }
        val butterflyCount = ids.count { it.startsWith("butterfly_") }

        assertTrue("has mammal badges", mammalCount > 0)
        assertTrue("has bird badges", birdCount > 0)
        assertTrue("has reptile badges", reptileCount > 0)
        assertTrue("has amphibian badges", amphibianCount > 0)
        assertTrue("has fish badges", fishCount > 0)
        assertTrue("has butterfly badges", butterflyCount > 0)
        assertEquals("sum equals 50", 50, mammalCount + birdCount + reptileCount + amphibianCount + fishCount + butterflyCount)
    }

    // ─────────────────────────────────────────────
    // TEST: getByBiome filters correctly
    // ─────────────────────────────────────────────
    @Test
    fun `getByBiome returns only badges for the requested biome`() = runTest {
        val testJson = buildBadgeCatalogJson()
        val (loader, _) = createLoaderWithJson(testJson)

        val all = loader.loadAll()

        val forestBadges = loader.getByBiome(all, "forest_friends")
        assertEquals("forest_friends count", 10, forestBadges.size)
        forestBadges.forEach { assertEquals("biome check", "forest_friends", it.biome) }

        val skyBadges = loader.getByBiome(all, "sky_scouts")
        assertEquals("sky_scouts count", 10, skyBadges.size)
        skyBadges.forEach { assertEquals("biome check", "sky_scouts", it.biome) }

        val emptyBiome = loader.getByBiome(all, "nonexistent")
        assertTrue("nonexistent biome returns empty", emptyBiome.isEmpty())
    }

    // ─────────────────────────────────────────────
    // TEST: every badge has all required fields populated
    // ─────────────────────────────────────────────
    @Test
    fun `every badge has non-empty required fields`() = runTest {
        val testJson = buildBadgeCatalogJson()
        val (loader, _) = createLoaderWithJson(testJson)

        val badges = loader.loadAll()

        badges.forEachIndexed { index, badge ->
            assertTrue("badge[$index] id non-blank: '${badge.id}'", badge.id.isNotBlank())
            assertTrue("badge[$index] biome non-blank", badge.biome.isNotBlank())
            assertTrue("badge[$index] name non-blank", badge.name.isNotBlank())
            assertTrue("badge[$index] title non-blank", badge.title.isNotBlank())
            assertTrue("badge[$index] funFact non-blank", badge.funFact.isNotBlank())
            assertTrue("badge[$index] emoji non-blank", badge.emoji.isNotBlank())
        }
    }

    // ─────────────────────────────────────────────
    // TEST: badges preserve order from JSON
    // ─────────────────────────────────────────────
    @Test
    fun `badges preserve original JSON ordering`() = runTest {
        val testJson = buildBadgeCatalogJson()
        val (loader, _) = createLoaderWithJson(testJson)

        val badges = loader.loadAll()

        // First badge should be mammal_tarsier, last should be butterfly_golden_birdwing
        assertEquals("first badge id", "mammal_tarsier", badges.first().id)
        assertEquals("last badge id", "butterfly_golden_birdwing", badges.last().id)
    }

    // ─────────────────────────────────────────────
    // TEST: all biomes represented (BadgeBiome enum IDs)
    // ─────────────────────────────────────────────
    @Test
    fun `all 5 biomes are represented in catalog`() = runTest {
        val testJson = buildBadgeCatalogJson()
        val (loader, _) = createLoaderWithJson(testJson)

        val badges = loader.loadAll()
        val biomeIds = badges.map { it.biome }.toSet()

        assertTrue("forest_friends", biomeIds.contains("forest_friends"))
        assertTrue("sky_scouts", biomeIds.contains("sky_scouts"))
        assertTrue("songbird_grove", biomeIds.contains("songbird_grove"))
        assertTrue("river_guardians", biomeIds.contains("river_guardians"))
        assertTrue("creek_coral", biomeIds.contains("creek_coral"))
        assertEquals("exactly 5 biomes", 5, biomeIds.size)
    }

    // ─── helpers ───

    /**
     * Creates a BadgeLoader backed by a mock Context/AssetManager that serves [json].
     * Returns both the loader and the mock AssetManager so callers can verify open() calls.
     */
    private fun createLoaderWithJson(json: String): Pair<BadgeLoader, AssetManager> {
        val mockContext = mockk<Context>()
        val mockAssets = mockk<AssetManager>()
        every { mockContext.assets } returns mockAssets

        val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
        every { mockAssets.open("badge_catalog.json") } returns inputStream

        val loader = BadgeLoader(mockContext)
        return Pair(loader, mockAssets)
    }

    /**
     * Builds the full badge_catalog.json content — 50 badges across 5 biomes.
     * Uses the real catalog data to ensure production-equivalent test coverage.
     */
    private fun buildBadgeCatalogJson(): String {
        return """
[
  {"id":"mammal_tarsier","biome":"forest_friends","name":"Philippine tarsier","title":"Moon-Eyed Jumper","fun_fact":"Each of its eyeballs is bigger than its brain — perfect for spotting bugs at night!","emoji":"🐒"},
  {"id":"mammal_tamaraw","biome":"forest_friends","name":"Tamaraw","title":"Mindoro Mini Buffalo","fun_fact":"Only about 500 tamaraws are left in the world — all living on one island, Mindoro!","emoji":"🐃"},
  {"id":"mammal_visayan_warty_pig","biome":"forest_friends","name":"Visayan warty pig","title":"Mohawk Forest Pig","fun_fact":"This pig rocks a spiky mohawk hairstyle and uses its snout to dig for tasty roots!","emoji":"🐗"},
  {"id":"mammal_spotted_deer","biome":"forest_friends","name":"Visayan spotted deer","title":"Spotted Forest Star","fun_fact":"Baby spotted deer are born covered in white dots — like a walking starry night sky!","emoji":"🦌"},
  {"id":"mammal_calamian_deer","biome":"forest_friends","name":"Calamian deer","title":"Low-Dash Deer","fun_fact":"This deer is a great swimmer and loves splashing between the Calamian islands!","emoji":"🦌"},
  {"id":"mammal_mouse_deer","biome":"forest_friends","name":"Philippine mouse-deer","title":"Tiny Stripe Sprinter","fun_fact":"It's one of the smallest hoofed animals on Earth — no bigger than a rabbit!","emoji":"🐭"},
  {"id":"mammal_pangolin","biome":"forest_friends","name":"Palawan pangolin","title":"Scaly Roller","fun_fact":"When scared, it rolls into a tight ball — a living pinecone that even tigers can't open!","emoji":"🦔"},
  {"id":"mammal_flying_fox","biome":"forest_friends","name":"Giant golden-crowned flying fox","title":"Golden Sky Giant","fun_fact":"Its wings stretch wider than a 6-year-old is tall — almost 1.7 meters from tip to tip!","emoji":"🦇"},
  {"id":"mammal_philippine_warty_pig","biome":"forest_friends","name":"Philippine warty pig","title":"Warty Snout Scout","fun_fact":"Those bumps on its face aren't really warts — they're padding to protect during piggy play fights!","emoji":"🐗"},
  {"id":"mammal_naked_backed_bat","biome":"forest_friends","name":"Philippine naked-backed fruit bat","title":"Back-Wing Bat","fun_fact":"Its wings meet in the middle of its back — making it look like it's wearing a tiny winged cape!","emoji":"🦇"},
  {"id":"bird_eagle","biome":"sky_scouts","name":"Philippine eagle","title":"Forest King","fun_fact":"It's our national bird! With a 2-meter wingspan, it's one of the largest and rarest eagles on Earth.","emoji":"🦅"},
  {"id":"bird_cockatoo","biome":"sky_scouts","name":"Philippine cockatoo","title":"Red-Tail Star","fun_fact":"This all-white parrot flashes bright red tail feathers when it flies — like a surprise firework!","emoji":"🦜"},
  {"id":"bird_peacock_pheasant","biome":"sky_scouts","name":"Palawan peacock-pheasant","title":"Sparkle Tail","fun_fact":"Its tail feathers shimmer with blue-green eye spots that look just like sparkling jewels!","emoji":"🦚"},
  {"id":"bird_duck","biome":"sky_scouts","name":"Philippine duck","title":"Rainbow Wing","fun_fact":"When sunlight hits its wings, they flash green, blue, and purple — a flying rainbow!","emoji":"🦆"},
  {"id":"bird_eagle_owl","biome":"sky_scouts","name":"Philippine eagle-owl","title":"Moonlight Guardian","fun_fact":"Its huge orange eyes can spot a mouse from a football field away — in almost total darkness!","emoji":"🦉"},
  {"id":"bird_luzon_scops_owl","biome":"sky_scouts","name":"Luzon scops owl","title":"Little Night Scout","fun_fact":"This tiny owl is only as tall as a pencil — but its hoot can echo through the whole forest!","emoji":"🦉"},
  {"id":"bird_rufous_hornbill","biome":"sky_scouts","name":"Rufous hornbill","title":"Big-Bill Banner","fun_fact":"Its bright red bill looks heavy but is actually hollow — like a lightweight trumpet made of keratin!","emoji":"🐦"},
  {"id":"bird_visayan_hornbill","biome":"sky_scouts","name":"Visayan hornbill","title":"Forest Trumpeter","fun_fact":"When it flies, its wings make a loud whooshing sound you can hear from 100 meters away!","emoji":"🐦"},
  {"id":"bird_mindoro_bleeding_heart","biome":"sky_scouts","name":"Mindoro bleeding-heart","title":"Heart Feather","fun_fact":"It has a bright red patch on its chest shaped just like a heart — nature's own Valentine!","emoji":"🕊️"},
  {"id":"bird_negros_bleeding_heart","biome":"sky_scouts","name":"Negros bleeding-heart","title":"Ruby Heart","fun_fact":"Its ruby-red chest feather glows in the sunlight, earning it the nickname 'the jewel of the forest.'","emoji":"🕊️"},
  {"id":"bird_cebu_flowerpecker","biome":"songbird_grove","name":"Cebu flowerpecker","title":"Cebu Scarlet Spot","fun_fact":"It's so rare that scientists thought it was extinct for 80 years — until someone spotted one in 1992!","emoji":"🐦"},
  {"id":"bird_scarlet_collared_flowerpecker","biome":"songbird_grove","name":"Scarlet-collared flowerpecker","title":"Mindoro Red Collar","fun_fact":"Its bright red collar looks like a tiny superhero cape — and it zips through flowers just as fast!","emoji":"🐦"},
  {"id":"bird_flame_breasted_fruit_dove","biome":"songbird_grove","name":"Flame-breasted fruit-dove","title":"Flame Chest Flyer","fun_fact":"Its chest blazes with fiery orange feathers — but it's a gentle fruit eater that wouldn't hurt a fly!","emoji":"🕊️"},
  {"id":"bird_black_hooded_coucal","biome":"songbird_grove","name":"Black-hooded coucal","title":"Forest Hood","fun_fact":"It wears a natural black hood like a mysterious forest detective — always watching from the bushes!","emoji":"🐦"},
  {"id":"bird_cebu_black_shama","biome":"songbird_grove","name":"Cebu black shama","title":"Cebu Song Star","fun_fact":"Its song is so beautiful and complex that it can copy the sounds of other birds and even car alarms!","emoji":"🐦"},
  {"id":"bird_philippine_creeper","biome":"songbird_grove","name":"Philippine creeper","title":"Stripe-Breast Climber","fun_fact":"It climbs tree trunks spiraling upward like a tiny feathered screwdriver — never sliding down!","emoji":"🐦"},
  {"id":"bird_philippine_trogon","biome":"songbird_grove","name":"Philippine trogon","title":"Ruby Belly","fun_fact":"Males have a bright pink-red belly so vivid that local people call it the 'forest flame.'","emoji":"🐦"},
  {"id":"bird_whiskered_pitta","biome":"songbird_grove","name":"Whiskered pitta","title":"Whisker Walker","fun_fact":"It has tiny feather whiskers near its beak — like a wise old forest wizard with a colorful robe!","emoji":"🐦"},
  {"id":"bird_palawan_tit","biome":"songbird_grove","name":"Palawan tit","title":"Palawan Sun Cap","fun_fact":"Its bright yellow cap looks like a tiny splash of sunshine — and it never sits still for long!","emoji":"🐦"},
  {"id":"bird_fairy_bluebird","biome":"songbird_grove","name":"Philippine fairy-bluebird","title":"Sapphire Spark","fun_fact":"Its feathers are an electric sapphire blue so intense it looks like it flew through a rainbow!","emoji":"🐦"},
  {"id":"reptile_philippine_crocodile","biome":"river_guardians","name":"Philippine crocodile","title":"River Guardian","fun_fact":"It's one of the rarest crocodiles on Earth — with a golden-brown color that sparkles in the sun!","emoji":"🐊"},
  {"id":"reptile_forest_turtle","biome":"river_guardians","name":"Philippine forest turtle","title":"Bow-Tie Turtle","fun_fact":"The pattern on its shell looks exactly like a gentleman's bow tie — very formal for a turtle!","emoji":"🐢"},
  {"id":"reptile_sierra_madre_monitor","biome":"river_guardians","name":"Northern Sierra Madre forest monitor","title":"Canopy Giant","fun_fact":"It can grow longer than a baseball bat and loves climbing high into the forest treetops!","emoji":"🦎"},
  {"id":"reptile_panay_monitor","biome":"river_guardians","name":"Panay monitor lizard","title":"Panay Tree Explorer","fun_fact":"This lizard is a champion climber — it spends most of its life in the trees eating fruit!","emoji":"🦎"},
  {"id":"reptile_grays_monitor","biome":"river_guardians","name":"Gray's monitor","title":"Forest Fruit Finder","fun_fact":"Unlike most lizards, this one loves eating fruit — making it a forest gardener that plants seeds!","emoji":"🦎"},
  {"id":"reptile_philippine_cobra","biome":"river_guardians","name":"Philippine cobra","title":"Hood Helper","fun_fact":"It can spread its neck into a wide hood wider than a dinner plate — but would rather slither away than fight!","emoji":"🐍"},
  {"id":"reptile_samar_cobra","biome":"river_guardians","name":"Samar cobra","title":"Island Hood","fun_fact":"Found only on Samar island, this cobra helps farmers by eating rats that damage rice fields!","emoji":"🐍"},
  {"id":"reptile_flying_lizard","biome":"river_guardians","name":"Philippine flying lizard","title":"Tree Glider","fun_fact":"It can't truly fly, but it glides between trees using skin flaps — like a tiny dragon with no fire!","emoji":"🦎"},
  {"id":"reptile_sailfin_lizard","biome":"river_guardians","name":"Philippine sailfin lizard","title":"Sail-Back Swimmer","fun_fact":"The fin on its back stands up like a sail when it swims — and it can even run across water!","emoji":"🦎"},
  {"id":"reptile_box_turtle","biome":"river_guardians","name":"Philippine box turtle","title":"Shell Door Keeper","fun_fact":"Its shell has special hinges that let it close up completely tight — like locking a tiny front door!","emoji":"🐢"},
  {"id":"amphibian_mindoro_tree_frog","biome":"creek_coral","name":"Mindoro tree frog","title":"Tree-Top Hopper","fun_fact":"Its sticky toe pads let it climb straight up tree trunks — no slipping, even in the rain!","emoji":"🐸"},
  {"id":"amphibian_mindanao_fanged_frog","biome":"creek_coral","name":"Mindanao fanged frog","title":"Forest Fang","fun_fact":"Those 'fangs' are actually little bone bumps on its jaw — it looks tough but only eats bugs!","emoji":"🐸"},
  {"id":"amphibian_flat_headed_frog","biome":"creek_coral","name":"Philippine flat-headed frog","title":"Stream Pancake","fun_fact":"Its head is so flat it can squeeze under rocks in fast streams — the original pancake frog!","emoji":"🐸"},
  {"id":"amphibian_luzon_narrow_mouthed_frog","biome":"creek_coral","name":"Luzon narrow-mouthed frog","title":"Little Pebble","fun_fact":"It's round and tiny like a pebble — perfect for hiding among rocks by the stream!","emoji":"🐸"},
  {"id":"amphibian_wrinkled_ground_frog","biome":"creek_coral","name":"Philippine wrinkled ground frog","title":"Wrinkle Walker","fun_fact":"Its wrinkly skin helps it blend into dead leaves on the forest floor — the master of disguise!","emoji":"🐸"},
  {"id":"fish_tawilis","biome":"creek_coral","name":"Tawilis","title":"Lake Silver Flash","fun_fact":"It's the only freshwater sardine in the world — found only in Taal Lake, nowhere else on Earth!","emoji":"🐟"},
  {"id":"fish_silver_therapon","biome":"creek_coral","name":"Silver therapon","title":"Silver Swimmer","fun_fact":"Its shiny silver body reflects light underwater like a mirror — helping it hide from predators!","emoji":"🐟"},
  {"id":"fish_sinarapan","biome":"creek_coral","name":"Sinarapan","title":"Tiny Glass Fish","fun_fact":"It's one of the smallest fish in the world — you could fit 10 of them on your thumb!","emoji":"🐟"},
  {"id":"butterfly_luzon_peacock_swallowtail","biome":"creek_coral","name":"Luzon peacock swallowtail","title":"Mountain Rainbow","fun_fact":"Its wings flash iridescent green and blue like a peacock feather — but this beauty lives high in the mountains!","emoji":"🦋"},
  {"id":"butterfly_golden_birdwing","biome":"creek_coral","name":"Golden birdwing","title":"Golden Glider","fun_fact":"Its gold-tipped wings are as wide as a small bird's — and they shimmer like liquid gold in flight!","emoji":"🦋"}
]
        """.trimIndent().trim()
    }
}
