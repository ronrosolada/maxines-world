# Maxine’s World: Philippine Endemic Badge Catalog

The 50-badge collection uses Philippine endemics across five collectible village biomes. The game’s animal-village theme and existing rewards module make this a natural progression layer. 

## Unlock model

### Daily Five-Subject Challenge

A learner earns exactly one animal badge only after completing one qualifying module in every subject during the same local calendar day:

1. English
2. Filipino
3. Mathematics
4. Science
5. Philippine History

The learner therefore completes five subject modules to earn one badge. The Achievements screen must display progress as `0/5` through `5/5`, with one clearly labeled subject slot for each requirement.

### Qualification rules

* A module counts only after the learner reaches its normal completion state and passes its configured assessment threshold.
* Each subject must contribute one distinct qualifying module completion on that day.
* Replaying a module already credited that day does not add progress.
* Additional modules in the same subject do not substitute for a missing subject.
* Completions cannot be combined across different calendar days.
* Partial progress resets at the start of the next local day, but the learner never loses badges already earned.
* A learner can earn no more than one animal badge per local calendar day.
* The badge grant must be idempotent. Retrying a database write, reopening the completion screen, changing device orientation, or synchronizing later must not grant a duplicate badge.
* Use the child profile’s configured time zone when available; otherwise use the device time zone. Store the resolved calendar date with the completed challenge so later time-zone changes do not alter past awards.

### Badge order and collection completion

* Award badges in the fixed catalog order unless a future product decision explicitly introduces learner choice.
* Use five collections of 10 badges: Forest Friends, Sky Scouts, Songbird Grove, River Guardians, and Creek and Coral.
* Completing the Daily Five-Subject Challenge on 50 separate days is required to collect all 50 badges.
* Give a collection a gold frame only after all 10 badges in that collection are earned.
* Keep badge art wordless; show the animal name and fun title in the UI, not inside the icon.

### Wildlife Field Guide collection mechanic

Present the complete 50-badge catalog from the beginning as a collectible Wildlife Field Guide. This should create the feeling of discovering and completing a creature collection while using original Maxine’s World terminology and visuals.

#### Unearned badge state

* Show all 50 badge positions from the first day so the learner can understand the size of the collection.
* Render every unearned badge as a muted silhouette or blank embossed icon inside its normal scalloped frame.
* Keep enough of the animal’s outline visible to create curiosity, but do not show its full colors, detailed illustration, common name, badge title, fact, or habitat description.
* Use a lock symbol only as a secondary cue. Do not cover the silhouette with a large lock.
* Label unearned entries with neutral copy such as “Undiscovered Animal” rather than the animal’s name.
* Provide an accessible content description such as “Undiscovered badge. Complete today’s five-subject challenge to discover an animal.”
* Do not make locked badges look broken, disabled, or unavailable for purchase.

#### Earned badge state

* Replace the silhouette with the full-color animal illustration immediately after the Daily Five-Subject Challenge is completed.
* Reveal the animal’s common name, badge title, collection, earned date, and one short child-friendly fact.
* Preserve the same grid position before and after earning so the learner can visually see the collection filling up.
* Add a subtle earned marker, such as a leaf checkmark or small golden star, without obscuring the artwork.
* Previously earned badges must remain fully visible offline and must never return to the locked state because of a sync or network failure.

#### Collection structure

* Display five collection pages or sections with 10 fixed slots each: Forest Friends, Sky Scouts, Songbird Grove, River Guardians, and Creek and Coral.
* Show progress at both levels: overall progress such as `12/50 discovered` and collection progress such as `Forest Friends 10/10`.
* Keep the badge sequence fixed. The next earned badge fills the next undiscovered slot in catalog order.
* When a learner completes a 10-badge collection, upgrade that collection’s header and earned badge frames to a gold treatment and show a one-time collection-complete celebration.
* When all 50 badges are earned, reveal a final Wildlife Guardian completion emblem. This emblem is a collection-completion cosmetic and does not replace any of the 50 animal badges.

#### Discovery reveal sequence

When the learner earns a badge, use this sequence:

1. Show the completed `5/5` subject tracker.
2. Transition to the next locked silhouette in the Field Guide.
3. Briefly pulse or glow the silhouette.
4. Reveal the full-color badge without using a loot-box, roulette, or random-chance presentation.
5. Display the animal name, badge title, and one child-friendly fact.
6. Show updated progress, for example `13 of 50 animals discovered`.
7. Provide one clear action to return to the village and a secondary action to view the Field Guide.

The awarded animal must be deterministic, not random. The learner should anticipate discovery and collection completion, but the app must not imitate gambling mechanics.

#### Recommended navigation and layout

* Add a child-facing `Wildlife Field Guide` or `Animal Badges` destination under the existing Achievements area.
* Default to a five-section overview showing collection art, collection name, and `earned/10` progress.
* Opening a collection shows a stable two-column phone grid or adaptive tablet grid of 10 badge slots.
* Tapping an earned badge opens its details. Tapping an unearned badge shows only the generic unlock requirement and current daily `x/5` progress.
* Include a filter for `All`, `Discovered`, and `Undiscovered`, but keep the fixed catalog order in every view.
* Ensure locked and unlocked states are distinguishable without relying on color alone. Use silhouette detail, labels, and accessibility descriptions.

### Celebration and motivation

* After each qualifying module, animate or mark only that subject’s slot and show supportive progress copy such as “Science complete — 3 of 5 subjects!”
* When all five subjects are complete, present a dedicated badge-reveal screen with the animal art, common name, badge title, and one short child-friendly fact.
* Do not use shame, loss, countdown pressure, or missed-day penalties. An incomplete day simply ends without a badge.
* Do not allow coins, parent actions, streaks, advertisements, or purchases to bypass the five-subject requirement.
* A streak may provide a cosmetic flourish or congratulatory message, but it must not grant extra badges or reduce the next day’s requirements.

### Terminology for implementation

The repository currently describes curriculum content using modules and lessons. For this mechanic, a “qualifying module” means one completed learning unit that the existing progress system recognizes as a unique, passed lesson/module completion. The implementation must use the project’s actual canonical content identifier rather than inventing a second parallel identifier.

## Forest Friends

| ID | Animal | Badge title |
|---|---|---|
| mammal_tarsier | Philippine tarsier | Moon-Eyed Jumper |
| mammal_tamaraw | Tamaraw | Mindoro Mini Buffalo |
| mammal_visayan_warty_pig | Visayan warty pig | Mohawk Forest Pig |
| mammal_philippine_warty_pig | Philippine warty pig | Warty Snout Scout |
| mammal_spotted_deer | Visayan spotted deer | Spotted Forest Star |
| mammal_calamian_deer | Calamian deer | Low-Dash Deer |
| mammal_mouse_deer | Philippine mouse-deer | Tiny Stripe Sprinter |
| mammal_pangolin | Palawan pangolin | Scaly Roller |
| mammal_flying_fox | Giant golden-crowned flying fox | Golden Sky Giant |
| mammal_naked_backed_bat | Philippine naked-backed fruit bat | Back-Wing Bat |

## Sky Scouts

| ID | Animal | Badge title |
|---|---|---|
| bird_eagle | Philippine eagle | Forest King |
| bird_cockatoo | Philippine cockatoo | Red-Tail Star |
| bird_peacock_pheasant | Palawan peacock-pheasant | Sparkle Tail |
| bird_duck | Philippine duck | Rainbow Wing |
| bird_eagle_owl | Philippine eagle-owl | Moonlight Guardian |
| bird_luzon_scops_owl | Luzon scops owl | Little Night Scout |
| bird_rufous_hornbill | Rufous hornbill | Big-Bill Banner |
| bird_visayan_hornbill | Visayan hornbill | Forest Trumpeter |
| bird_mindoro_bleeding_heart | Mindoro bleeding-heart | Heart Feather |
| bird_negros_bleeding_heart | Negros bleeding-heart | Ruby Heart |

## Songbird Grove

| ID | Animal | Badge title |
|---|---|---|
| bird_cebu_flowerpecker | Cebu flowerpecker | Cebu Scarlet Spot |
| bird_scarlet_collared_flowerpecker | Scarlet-collared flowerpecker | Mindoro Red Collar |
| bird_flame_breasted_fruit_dove | Flame-breasted fruit-dove | Flame Chest Flyer |
| bird_black_hooded_coucal | Black-hooded coucal | Forest Hood |
| bird_cebu_black_shama | Cebu black shama | Cebu Song Star |
| bird_philippine_creeper | Philippine creeper | Stripe-Breast Climber |
| bird_philippine_trogon | Philippine trogon | Ruby Belly |
| bird_whiskered_pitta | Whiskered pitta | Whisker Walker |
| bird_palawan_tit | Palawan tit | Palawan Sun Cap |
| bird_fairy_bluebird | Philippine fairy-bluebird | Sapphire Spark |

## River Guardians

| ID | Animal | Badge title |
|---|---|---|
| reptile_philippine_crocodile | Philippine crocodile | River Guardian |
| reptile_forest_turtle | Philippine forest turtle | Bow-Tie Turtle |
| reptile_sierra_madre_monitor | Northern Sierra Madre forest monitor | Canopy Giant |
| reptile_panay_monitor | Panay monitor lizard | Panay Tree Explorer |
| reptile_grays_monitor | Gray’s monitor | Forest Fruit Finder |
| reptile_philippine_cobra | Philippine cobra | Hood Helper |
| reptile_samar_cobra | Samar cobra | Island Hood |
| reptile_flying_lizard | Philippine flying lizard | Tree Glider |
| reptile_sailfin_lizard | Philippine sailfin lizard | Sail-Back Swimmer |
| reptile_box_turtle | Philippine box turtle | Shell Door Keeper |

## Creek and Coral

| ID | Animal | Badge title |
|---|---|---|
| amphibian_mindoro_tree_frog | Mindoro tree frog | Tree-Top Hopper |
| amphibian_mindanao_fanged_frog | Mindanao fanged frog | Forest Fang |
| amphibian_flat_headed_frog | Philippine flat-headed frog | Stream Pancake |
| amphibian_luzon_narrow_mouthed_frog | Luzon narrow-mouthed frog | Little Pebble |
| amphibian_wrinkled_ground_frog | Philippine wrinkled ground frog | Wrinkle Walker |
| fish_tawilis | Tawilis | Lake Silver Flash |
| fish_silver_therapon | Silver therapon | Silver Swimmer |
| fish_sinarapan | Sinarapan | Tiny Glass Fish |
| butterfly_luzon_peacock_swallowtail | Luzon peacock swallowtail | Mountain Rainbow |
| butterfly_golden_birdwing | Golden birdwing | Golden Glider |

The Philippines has exceptional endemism across mammals, birds, reptiles, amphibians, freshwater fish, and insects, which supports using this collection as both a reward system and a light conservation-learning layer. 

## Art direction

* Cartoon storybook animals with oversized, warm eyes and rounded silhouettes.
* Scalloped collectible-token frame, bold dark-brown outline, and friendly village or habitat vignette.
* Tropical palette: leaf green, mango yellow, coral orange, sky blue, cream, and warm brown.
* No text inside icons; optimize each badge for recognition at 64 px.


---

## Sources

- [GitHub - ronrosolada/maxines-world: Maxine's World — Private Android learning app for Grade 3 (MATATAG-aligned). Offline-first, animal-village themed. Kotlin, Jetpack Compose, Room, Hilt. · GitHub](https://github.com/ronrosolada/maxines-world)
- [Philippines - Species](https://www.cepf.net/our-work/biodiversity-hotspots/philippines/species)
