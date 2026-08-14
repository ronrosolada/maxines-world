package com.maxinesworld.featurerewards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coredesignsystem.theme.Ink

/** Static references keep R8/resource shrinking from removing manifest-selected photos. */
private val BADGE_PHOTO_RESOURCES = mapOf(
    "animal_photo_amphibian_flat_headed_frog" to R.drawable.animal_photo_amphibian_flat_headed_frog,
    "animal_photo_amphibian_luzon_narrow_mouthed_frog" to R.drawable.animal_photo_amphibian_luzon_narrow_mouthed_frog,
    "animal_photo_amphibian_mindanao_fanged_frog" to R.drawable.animal_photo_amphibian_mindanao_fanged_frog,
    "animal_photo_amphibian_mindoro_tree_frog" to R.drawable.animal_photo_amphibian_mindoro_tree_frog,
    "animal_photo_amphibian_wrinkled_ground_frog" to R.drawable.animal_photo_amphibian_wrinkled_ground_frog,
    "animal_photo_bird_black_hooded_coucal" to R.drawable.animal_photo_bird_black_hooded_coucal,
    "animal_photo_bird_cebu_black_shama" to R.drawable.animal_photo_bird_cebu_black_shama,
    "animal_photo_bird_cebu_flowerpecker" to R.drawable.animal_photo_bird_cebu_flowerpecker,
    "animal_photo_bird_cockatoo" to R.drawable.animal_photo_bird_cockatoo,
    "animal_photo_bird_duck" to R.drawable.animal_photo_bird_duck,
    "animal_photo_bird_eagle" to R.drawable.animal_photo_bird_eagle,
    "animal_photo_bird_eagle_owl" to R.drawable.animal_photo_bird_eagle_owl,
    "animal_photo_bird_fairy_bluebird" to R.drawable.animal_photo_bird_fairy_bluebird,
    "animal_photo_bird_flame_breasted_fruit_dove" to R.drawable.animal_photo_bird_flame_breasted_fruit_dove,
    "animal_photo_bird_luzon_scops_owl" to R.drawable.animal_photo_bird_luzon_scops_owl,
    "animal_photo_bird_mindoro_bleeding_heart" to R.drawable.animal_photo_bird_mindoro_bleeding_heart,
    "animal_photo_bird_negros_bleeding_heart" to R.drawable.animal_photo_bird_negros_bleeding_heart,
    "animal_photo_bird_palawan_tit" to R.drawable.animal_photo_bird_palawan_tit,
    "animal_photo_bird_peacock_pheasant" to R.drawable.animal_photo_bird_peacock_pheasant,
    "animal_photo_bird_philippine_creeper" to R.drawable.animal_photo_bird_philippine_creeper,
    "animal_photo_bird_philippine_trogon" to R.drawable.animal_photo_bird_philippine_trogon,
    "animal_photo_bird_rufous_hornbill" to R.drawable.animal_photo_bird_rufous_hornbill,
    "animal_photo_bird_scarlet_collared_flowerpecker" to R.drawable.animal_photo_bird_scarlet_collared_flowerpecker,
    "animal_photo_bird_visayan_hornbill" to R.drawable.animal_photo_bird_visayan_hornbill,
    "animal_photo_bird_whiskered_pitta" to R.drawable.animal_photo_bird_whiskered_pitta,
    "animal_photo_butterfly_golden_birdwing" to R.drawable.animal_photo_butterfly_golden_birdwing,
    "animal_photo_butterfly_luzon_peacock_swallowtail" to R.drawable.animal_photo_butterfly_luzon_peacock_swallowtail,
    "animal_photo_fish_silver_therapon" to R.drawable.animal_photo_fish_silver_therapon,
    "animal_photo_fish_sinarapan" to R.drawable.animal_photo_fish_sinarapan,
    "animal_photo_fish_tawilis" to R.drawable.animal_photo_fish_tawilis,
    "animal_photo_mammal_calamian_deer" to R.drawable.animal_photo_mammal_calamian_deer,
    "animal_photo_mammal_flying_fox" to R.drawable.animal_photo_mammal_flying_fox,
    "animal_photo_mammal_mouse_deer" to R.drawable.animal_photo_mammal_mouse_deer,
    "animal_photo_mammal_naked_backed_bat" to R.drawable.animal_photo_mammal_naked_backed_bat,
    "animal_photo_mammal_pangolin" to R.drawable.animal_photo_mammal_pangolin,
    "animal_photo_mammal_philippine_warty_pig" to R.drawable.animal_photo_mammal_philippine_warty_pig,
    "animal_photo_mammal_spotted_deer" to R.drawable.animal_photo_mammal_spotted_deer,
    "animal_photo_mammal_tamaraw" to R.drawable.animal_photo_mammal_tamaraw,
    "animal_photo_mammal_tarsier" to R.drawable.animal_photo_mammal_tarsier,
    "animal_photo_mammal_visayan_warty_pig" to R.drawable.animal_photo_mammal_visayan_warty_pig,
    "animal_photo_reptile_box_turtle" to R.drawable.animal_photo_reptile_box_turtle,
    "animal_photo_reptile_flying_lizard" to R.drawable.animal_photo_reptile_flying_lizard,
    "animal_photo_reptile_forest_turtle" to R.drawable.animal_photo_reptile_forest_turtle,
    "animal_photo_reptile_grays_monitor" to R.drawable.animal_photo_reptile_grays_monitor,
    "animal_photo_reptile_philippine_cobra" to R.drawable.animal_photo_reptile_philippine_cobra,
    "animal_photo_reptile_philippine_crocodile" to R.drawable.animal_photo_reptile_philippine_crocodile,
    "animal_photo_reptile_sailfin_lizard" to R.drawable.animal_photo_reptile_sailfin_lizard,
    "animal_photo_reptile_samar_cobra" to R.drawable.animal_photo_reptile_samar_cobra,
    "animal_photo_reptile_sierra_madre_monitor" to R.drawable.animal_photo_reptile_sierra_madre_monitor,
)

@Composable
internal fun BadgePhotoArtwork(
    badge: CollectibleBadge,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    @Suppress("DiscouragedApi")
    val resourceId = remember(badge.photoAsset, context.packageName) {
        badge.photoAsset?.let { asset ->
            BADGE_PHOTO_RESOURCES[asset]
                ?: context.resources.getIdentifier(asset, "drawable", context.packageName)
        } ?: 0
    }

    if (resourceId != 0) {
        Image(
            painter = painterResource(resourceId),
            contentDescription = if (badge.photoKind == "official_stamp") {
                "Official stamp of ${badge.name}"
            } else {
                "Real-life photo of ${badge.name}"
            },
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(Color.Black.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp),
            ) {
                Icon(Icons.Default.Photo, contentDescription = null, tint = Ink.copy(alpha = 0.35f))
                Text(
                    "Real-life photo coming soon",
                    color = Ink.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

internal fun badgePhotoKindLabel(kind: String?): String = when (kind) {
    "museum_specimen_photo" -> "Museum specimen photo"
    "captive_photo" -> "Photo from a wildlife facility"
    "field_photo" -> "Field photo"
    "market_photo" -> "Market photo"
    "official_stamp" -> "Official Philippine stamp"
    else -> "Photo reference"
}
