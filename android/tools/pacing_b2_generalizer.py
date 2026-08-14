#!/usr/bin/env python3
"""Wave B2: differentiate the remaining 18 over-fanned objective groups.

For each lesson, replaces the shared objective/title/introduction with
per-lesson focus grounded in the lesson's own assessment content (anchor
place/material/factors/vocabulary/story). Activity instructions and keys
are untouched. Idempotent via the pacingPass marker.
"""
import json
import os
import sys

ROOT = "/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons"
MARKER = "pacing-b2-2026-08-15"

# ── per-group hand-authored meta: filename -> (title, objective, introduction) ──

KAPALIGIRAN = {
    "makabansa-g3-q2-w02-d01.json": ("Pamumuhay sa Baybayin",
        "Naipaliliwanag kung paano nakaaapekto ang baybayin sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! 🏝️ Pasyalan natin ang isang komunidad sa baybayin. Paano kaya binabago ng dagat ang paraan ng pamumuhay ng mga tao doon? Sama ka kay Milo!"),
    "makabansa-g3-q2-w02-d04.json": ("Pamumuhay sa Kabundukan",
        "Naipaliliwanag kung paano nakaaapekto ang kabundukan sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! ⛰️ May komunidad sa kabundukan na gustong ipakita ni Milo. Paano kaya nabubuhay ang mga tao roon? Tara, alamin natin!"),
    "makabansa-g3-q4-w04-d03.json": ("Pamumuhay sa Ilog",
        "Naipaliliwanag kung paano nakaaapekto ang ilog sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! 🛶 Sa tabi ng ilog may masayang komunidad. Paano nila ginagamit ang ilog sa pang-araw-araw? Halika, tingnan natin!"),
    "makabansa-g3-q4-w04-d04.json": ("Pamumuhay sa Tahanan",
        "Naipaliliwanag kung paano nakaaapekto ang paligid sa pamumuhay sa tahanan ng komunidad.",
        "Kumusta, Maxine! 🏠 Magsimula tayo sa pinakamalapit: ang tahanan. Paano hinuhubog ng paligid ang pamumuhay ng pamilya? Alamin natin!"),
    "makabansa-g3-q4-w05-d01.json": ("Pamumuhay sa Kapatagan",
        "Naipaliliwanag kung paano nakaaapekto ang kapatagan sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! 🌾 Sa malawak na kapatagan, maraming sakahan. Paano kaya nakatutulong ang lupain sa pamumuhay ng mga tao? Tara na!"),
    "makabansa-g3-q4-w05-d02.json": ("Pamumuhay sa Lambak",
        "Naipaliliwanag kung paano nakaaapekto ang lambak sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! 🏞️ Ang lambak ay mababa at napapaligiran ng bundok. Paano kaya namumuhay ang komunidad doon? Sama ka kay Milo!"),
    "makabansa-g3-q4-w05-d03.json": ("Pamumuhay sa Paaralan",
        "Naipaliliwanag kung paano nakaaapekto ang paligid sa pamumuhay sa paaralan ng komunidad.",
        "Kumusta, Maxine! 🏫 Kahit sa paaralan, may epekto ang paligid. Ano kaya ito? Tuklasin natin kasama si Milo!"),
    "makabansa-g3-q4-w05-d04.json": ("Pamumuhay sa Talampas",
        "Naipaliliwanag kung paano nakaaapekto ang talampas sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! 🗻 Ang talampas ay mataas na lupaing patag. Paano kaya namumuhay ang mga tao roon? Alamin natin!"),
    "makabansa-g3-q4-w06-d01.json": ("Pamumuhay sa Isla",
        "Naipaliliwanag kung paano nakaaapekto ang isla sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! 🏝️ Nasa gitna ng dagat ang isla. Paano kaya ang pamumuhay ng komunidad doon? Tara, tingnan natin!"),
    "makabansa-g3-q4-w06-d02.json": ("Pamumuhay sa Siyudad",
        "Naipaliliwanag kung paano nakaaapekto ang siyudad sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! 🏙️ Sa siyudad, maraming tao at gusali. Paano kaya hinuhubog nito ang pamumuhay? Sama ka kay Milo!"),
    "makabansa-g3-q4-w06-d03.json": ("Pamumuhay sa Pamilihan",
        "Naipaliliwanag kung paano nakaaapekto ang paligid sa pamumuhay sa pamilihan ng komunidad.",
        "Kumusta, Maxine! 🧺 Sa pamilihan, makikita ang buhay ng komunidad. Ano kaya ang kuwento ng mga nagtitinda? Alamin natin!"),
    "makabansa-g3-q4-w06-d04.json": ("Pamumuhay sa Kabayanan",
        "Naipaliliwanag kung paano nakaaapekto ang kabayanan sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! 🏘️ Ang kabayanan ay buhay na buhay. Paano kaya hinuhubog ng paligid ang pamumuhay doon? Tara na!"),
    "makabansa-g3-q4-w07-d01.json": ("Pamumuhay sa Probinsiya",
        "Naipaliliwanag kung paano nakaaapekto ang probinsiya sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! 🌄 Sa probinsiya, simple at payapa ang buhay. Paano kaya nakaangkop ang mga tao sa kanilang paligid? Sama ka kay Milo!"),
    "makabansa-g3-q4-w07-d02.json": ("Pamumuhay sa Bukid",
        "Naipaliliwanag kung paano nakaaapekto ang bukid sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! 🚜 Sa bukid, maraming pananim. Paano kaya nakakonekta ang lupa sa buhay ng mga tao? Alamin natin!"),
    "makabansa-g3-q4-w07-d03.json": ("Pamumuhay sa Daungan",
        "Naipaliliwanag kung paano nakaaapekto ang daungan sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! ⚓ Sa daungan, dumarating ang mga barko. Paano kaya nakatutulong ito sa komunidad? Tuklasin natin!"),
    "makabansa-g3-q4-w07-d04.json": ("Pamumuhay sa Lawa",
        "Naipaliliwanag kung paano nakaaapekto ang lawa sa kultura at pamumuhay ng komunidad.",
        "Kumusta, Maxine! 🎣 Sa tabi ng lawa, may pamayanang namumuhay mula sa tubig. Paano kaya? Tara na kay Milo!"),
}

PAGGALANG = {
    "gmrc-g3-q1-w01-d02.json": ("Magalang sa Komunidad",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa komunidad.",
        "Kumusta, Maxine! 🙌 Sa komunidad, marami tayong nakakasalamuha. Paano tayo magiging magalang sa salita, kilos, at pakikinig? Tara na!"),
    "gmrc-g3-q1-w02-d01.json": ("Magalang sa Bahay",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa loob ng bahay.",
        "Kumusta, Maxine! 🏠 Sa ating tahanan, nagsisimula ang paggalang. Paano tayo magiging magalang sa pamilya? Alamin natin!"),
    "gmrc-g3-q2-w03-d01.json": ("Magalang sa Palengke",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa palengke.",
        "Kumusta, Maxine! 🧺 Sa palengke, maraming tao. Paano tayo magiging magalang sa mga tindera at mamimili? Tara na!"),
    "gmrc-g3-q2-w03-d02.json": ("Magalang sa Parke",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa parke.",
        "Kumusta, Maxine! 🌳 Sa parke, maraming naglalaro. Paano tayo magiging magalang sa iba? Sama ka kay Milo!"),
    "gmrc-g3-q2-w03-d03.json": ("Magalang sa Paaralan",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa paaralan.",
        "Kumusta, Maxine! 🏫 Sa paaralan, natututo tayong maging magalang sa guro at kaklase. Paano? Alamin natin!"),
    "gmrc-g3-q2-w03-d04.json": ("Magalang sa Plasa",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa plasa.",
        "Kumusta, Maxine! 🎪 Sa plasa, may mga matatanda at bata. Paano tayo magiging magalang sa kanila? Tuklasin natin!"),
    "gmrc-g3-q2-w03-d05.json": ("Magalang sa Hardin",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa hardin.",
        "Kumusta, Maxine! 🌷 Sa hardin, tahimik at payapa. Paano tayo magiging magalang doon? Tara na kay Milo!"),
    "gmrc-g3-q2-w04-d01.json": ("Magalang sa Tarangkahan",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa tarangkahan.",
        "Kumusta, Maxine! 🚪 Sa tarangkahan, may mga bisitang dumarating. Paano natin sila igagalang? Alamin natin!"),
    "gmrc-g3-q3-w05-d01.json": ("Magalang sa Bakuran",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa bakuran.",
        "Kumusta, Maxine! 🌿 Sa bakuran, may mga kapitbahay tayong nakakasama. Paano tayo magiging magalang? Tara na!"),
    "gmrc-g3-q3-w05-d03.json": ("Magalang sa Silid-Aklatan",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa silid-aklatan.",
        "Kumusta, Maxine! 📚 Sa silid-aklatan, kailangan ang tahimik na pakikinig. Paano pa tayo magiging magalang? Alamin natin!"),
    "gmrc-g3-q3-w05-d05.json": ("Magalang sa Kantina",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa kantina.",
        "Kumusta, Maxine! 🍚 Sa kantina, maraming pumipila. Paano tayo magiging magalang habang bumibili? Tara na!"),
    "gmrc-g3-q4-w07-d01.json": ("Magalang sa Tindahan",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa tindahan.",
        "Kumusta, Maxine! 🏪 Sa tindahan, may tindera tayong kinakausap. Paano tayo magiging magalang? Alamin natin!"),
    "gmrc-g3-q4-w07-d02.json": ("Magalang sa Daan Pauwi",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa daan pauwi.",
        "Kumusta, Maxine! 🚶 Sa daan pauwi, may mga nakakasalubong tayo. Paano tayo magiging magalang? Tuklasin natin!"),
    "gmrc-g3-q4-w07-d05.json": ("Magalang sa Palaruan",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa palaruan.",
        "Kumusta, Maxine! ⚽ Sa palaruan, sama-samang naglalaro. Paano tayo magiging magalang sa kalaro? Tara na!"),
    "gmrc-g3-q4-w08-d01.json": ("Magalang sa Kapitbahayan",
        "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa sa kapitbahayan.",
        "Kumusta, Maxine! 🏘️ Sa kapitbahayan, may mga matatanda at kaibigan tayo. Paano natin sila igagalang? Alamin natin!"),
}

MATERIALS = {
    "science-g3-q1-w01-d02.json": ("Wooden Ruler Detective",
        "Describe the observable properties of a wooden ruler and choose its safe use or handling.",
        "Hi Maxine! 🔍 Milo found a wooden ruler. Let's look closely: is it hard or soft, bendy or stiff? And how do we use it safely?"),
    "science-g3-q1-w01-d03.json": ("Stone Detective",
        "Describe the observable properties of a stone and choose its safe use or handling.",
        "Hi Maxine! 🔍 Milo found a stone. Let's look closely: is it hard or soft, heavy or light? And how do we handle it safely?"),
    "science-g3-q1-w01-d04.json": ("Plastic Cup Detective",
        "Describe the observable properties of a plastic cup and choose its safe use or handling.",
        "Hi Maxine! 🔍 Milo found a plastic cup. Let's look closely: is it bendy or stiff, clear or colored? And how do we use it safely?"),
    "science-g3-q1-w01-d05.json": ("Metal Key Detective",
        "Describe the observable properties of a metal key and choose its safe use or handling.",
        "Hi Maxine! 🔍 Milo found a metal key. Let's look closely: is it shiny or dull, hard or soft? And how do we handle it safely?"),
    "science-g3-q1-w02-d01.json": ("Glass Window Detective",
        "Describe the observable properties of a glass window and choose its safe use or handling.",
        "Hi Maxine! 🔍 Milo found a glass window. Let's look closely: is it clear or cloudy, hard or soft? And how do we stay safe near it?"),
    "science-g3-q1-w02-d02.json": ("Sandpaper Detective",
        "Describe the observable properties of sandpaper and choose its safe use or handling.",
        "Hi Maxine! 🔍 Milo found sandpaper. Let's look closely: is it rough or smooth? And how do we use it safely?"),
    "science-g3-q3-w05-d02.json": ("Paper Bag Detective",
        "Describe the observable properties of a paper bag and choose its safe use or handling.",
        "Hi Maxine! 🔍 Milo found a paper bag. Let's look closely: is it strong or weak, smooth or rough? And how do we use it safely?"),
    "science-g3-q3-w05-d03.json": ("Chalk Detective",
        "Describe the observable properties of chalk and choose its safe use or handling.",
        "Hi Maxine! 🔍 Milo found a piece of chalk. Let's look closely: is it hard or crumbly? And how do we handle it safely?"),
    "science-g3-q3-w06-d02.json": ("Crayon Detective",
        "Describe the observable properties of a crayon and choose its safe use or handling.",
        "Hi Maxine! 🔍 Milo found a crayon. Let's look closely: is it smooth or rough, soft or hard? And how do we use it safely?"),
    "science-g3-q4-w07-d03.json": ("Glass Cup Detective",
        "Describe the observable properties of a glass cup and choose its safe use or handling.",
        "Hi Maxine! 🔍 Milo found a glass cup. Let's look closely: is it clear or cloudy? And how do we handle it safely?"),
    "science-g3-q4-w07-d04.json": ("Paper Plate Detective",
        "Describe the observable properties of a paper plate and choose its safe use or handling.",
        "Hi Maxine! 🔍 Milo found a paper plate. Let's look closely: is it stiff or floppy? And how do we use it safely?"),
}

ADDITION = {
    "mathematics-g3-q2-w03-d02.json": ("Adding 245 and 123",
        "Add 245 and 123 using place value, with or without regrouping.",
        "Hi Maxine! ➕ Milo has 245 and 123 mangoes in two baskets. Help him find the total!"),
    "mathematics-g3-q2-w03-d03.json": ("Adding 432 and 516",
        "Add 432 and 516 using place value, with or without regrouping.",
        "Hi Maxine! ➕ Milo counted 432 shells and 516 more shells. How many shells altogether?"),
    "mathematics-g3-q2-w03-d04.json": ("Adding 671 and 229",
        "Add 671 and 229 using place value, with or without regrouping.",
        "Hi Maxine! ➕ Milo picked 671 flowers and 229 more flowers. How many flowers in all?"),
    "mathematics-g3-q2-w03-d05.json": ("Adding 118 and 264",
        "Add 118 and 264 using place value, with or without regrouping.",
        "Hi Maxine! ➕ Milo has 118 stars and 264 more stars. How many stars altogether?"),
    "mathematics-g3-q2-w04-d01.json": ("Adding 573 and 248",
        "Add 573 and 248 using place value, with or without regrouping.",
        "Hi Maxine! ➕ Milo counted 573 fish and 248 more fish in the pond. How many fish in all?"),
    "mathematics-g3-q2-w04-d04.json": ("Adding 306 and 489",
        "Add 306 and 489 using place value, with or without regrouping.",
        "Hi Maxine! ➕ Milo has 306 seeds and 489 more seeds. How many seeds altogether?"),
    "mathematics-g3-q2-w04-d05.json": ("Adding 752 and 168",
        "Add 752 and 168 using place value, with or without regrouping.",
        "Hi Maxine! ➕ Milo counted 752 leaves and 168 more leaves. How many leaves in all?"),
    "mathematics-g3-q3-w06-d04.json": ("Adding 895 and 107",
        "Add 895 and 107 using place value, with or without regrouping.",
        "Hi Maxine! ➕ Milo has 895 buttons and 107 more buttons. How many buttons altogether?"),
    "mathematics-g3-q4-w08-d04.json": ("Adding 456 and 544",
        "Add 456 and 544 using place value, with or without regrouping.",
        "Hi Maxine! ➕ Milo counted 456 birds and 544 more birds. How many birds in all?"),
}

MULTIPLY = {
    "mathematics-g3-q3-w05-d04.json": ("Multiplying 6 by 5",
        "Multiply 6 by 5 using equal groups and place value.",
        "Hi Maxine! ✖️ Milo made 6 baskets with 5 mangoes each. How many mangoes altogether?"),
    "mathematics-g3-q3-w06-d01.json": ("Multiplying 5 by 7",
        "Multiply 5 by 7 using equal groups and place value.",
        "Hi Maxine! ✖️ Milo arranged 5 rows with 7 shells each. How many shells in all?"),
    "mathematics-g3-q3-w06-d03.json": ("Multiplying 8 by 4",
        "Multiply 8 by 4 using equal groups and place value.",
        "Hi Maxine! ✖️ Milo planted 8 plots with 4 flowers each. How many flowers altogether?"),
    "mathematics-g3-q3-w07-d01.json": ("Multiplying 7 by 6",
        "Multiply 7 by 6 using equal groups and place value.",
        "Hi Maxine! ✖️ Milo drew 7 groups with 6 stars each. How many stars in all?"),
    "mathematics-g3-q3-w07-d02.json": ("Multiplying 9 by 3",
        "Multiply 9 by 3 using equal groups and place value.",
        "Hi Maxine! ✖️ Milo counted 9 jars with 3 fireflies each. How many fireflies altogether?"),
    "mathematics-g3-q4-w08-d01.json": ("Multiplying 6 by 8",
        "Multiply 6 by 8 using equal groups and place value.",
        "Hi Maxine! ✖️ Milo made 6 bags with 8 candies each. How many candies in all?"),
    "mathematics-g3-q4-w08-d02.json": ("Multiplying 7 by 7",
        "Multiply 7 by 7 using equal groups and place value.",
        "Hi Maxine! ✖️ Milo counted 7 branches with 7 leaves each. How many leaves altogether?"),
    "mathematics-g3-q4-w08-d03.json": ("Multiplying 8 by 8",
        "Multiply 8 by 8 using equal groups and place value.",
        "Hi Maxine! ✖️ Milo set 8 tables with 8 plates each. How many plates in all?"),
    "mathematics-g3-q4-w09-d01.json": ("Multiplying 9 by 4",
        "Multiply 9 by 4 using equal groups and place value.",
        "Hi Maxine! ✖️ Milo painted 9 boards with 4 dots each. How many dots altogether?"),
}

WORDS = {
    "english-g3-q2-w01-d01.json": ("Brave, Huge, Whisper",
        "Use the words brave, huge, and whisper in context with sentence clues.",
        "Hi Maxine! 📖 Today Milo brought three new words: brave, huge, and whisper. Let's find what they mean and use them in sentences!"),
    "english-g3-q2-w01-d02.json": ("Gentle, Clever, Cozy",
        "Use the words gentle, clever, and cozy in context with sentence clues.",
        "Hi Maxine! 📖 Today Milo brought three new words: gentle, clever, and cozy. Let's find what they mean and use them in sentences!"),
    "english-g3-q2-w01-d03.json": ("Curious, Proud, Silent",
        "Use the words curious, proud, and silent in context with sentence clues.",
        "Hi Maxine! 📖 Today Milo brought three new words: curious, proud, and silent. Let's find what they mean and use them in sentences!"),
    "english-g3-q2-w01-d04.json": ("Delicious, Eager, Fresh",
        "Use the words delicious, eager, and fresh in context with sentence clues.",
        "Hi Maxine! 📖 Today Milo brought three new words: delicious, eager, and fresh. Let's find what they mean and use them in sentences!"),
    "english-g3-q3-w08-d01.json": ("Bright, Careful, Honest",
        "Use the words bright, careful, and honest in context with sentence clues.",
        "Hi Maxine! 📖 Today Milo brought three new words: bright, careful, and honest. Let's find what they mean and use them in sentences!"),
    "english-g3-q3-w08-d02.json": ("Busy, Cheerful, Deep",
        "Use the words busy, cheerful, and deep in context with sentence clues.",
        "Hi Maxine! 📖 Today Milo brought three new words: busy, cheerful, and deep. Let's find what they mean and use them in sentences!"),
    "english-g3-q3-w08-d03.json": ("Silly, Warm, Quiet",
        "Use the words silly, warm, and quiet in context with sentence clues.",
        "Hi Maxine! 📖 Today Milo brought three new words: silly, warm, and quiet. Let's find what they mean and use them in sentences!"),
}

WASTONG = {
    "filipino-g3-q1-w01-d04.json": ("Baybay: Paaralan",
        "Naisusulat nang wasto ang salitang 'paaralan' at nagagamit ito sa pangungusap.",
        "Kumusta, Maxine! ✏️ Ngayon, isusulat natin nang tama ang salitang 'paaralan'. Sama ka kay Milo!"),
    "filipino-g3-q2-w04-d01.json": ("Baybay: Aklatan",
        "Naisusulat nang wasto ang salitang 'aklatan' at nagagamit ito sa pangungusap.",
        "Kumusta, Maxine! ✏️ Ngayon, isusulat natin nang tama ang salitang 'aklatan'. Sama ka kay Milo!"),
    "filipino-g3-q2-w05-d01.json": ("Baybay: Bintana",
        "Naisusulat nang wasto ang salitang 'bintana' at nagagamit ito sa pangungusap.",
        "Kumusta, Maxine! ✏️ Ngayon, isusulat natin nang tama ang salitang 'bintana'. Sama ka kay Milo!"),
    "filipino-g3-q3-w08-d01.json": ("Baybay: Pangarap",
        "Naisusulat nang wasto ang salitang 'pangarap' at nagagamit ito sa pangungusap.",
        "Kumusta, Maxine! ✏️ Ngayon, isusulat natin nang tama ang salitang 'pangarap'. Sama ka kay Milo!"),
    "filipino-g3-q3-w08-d05.json": ("Baybay: Pagkain",
        "Naisusulat nang wasto ang salitang 'pagkain' at nagagamit ito sa pangungusap.",
        "Kumusta, Maxine! ✏️ Ngayon, isusulat natin nang tama ang salitang 'pagkain'. Sama ka kay Milo!"),
    "filipino-g3-q4-w11-d02.json": ("Baybay: Ilog",
        "Naisusulat nang wasto ang salitang 'ilog' at nagagamit ito sa pangungusap.",
        "Kumusta, Maxine! ✏️ Ngayon, isusulat natin nang tama ang salitang 'ilog'. Sama ka kay Milo!"),
    "filipino-g3-q4-w12-d02.json": ("Baybay: Suklay",
        "Naisusulat nang wasto ang salitang 'suklay' at nagagamit ito sa pangungusap.",
        "Kumusta, Maxine! ✏️ Ngayon, isusulat natin nang tama ang salitang 'suklay'. Sama ka kay Milo!"),
}

LIVING = {
    "science-g3-q1-w01-d01.json": ("Mango Tree and Dog",
        "Classify a mango tree and a dog as living things and describe a basic need or body part.",
        "Hi Maxine! 🌿 Milo wants to check: is a mango tree living? Is a dog living? Let's find out what living things need!"),
    "science-g3-q2-w03-d02.json": ("Bird and Fish",
        "Classify a bird and a fish as living things and describe a basic need or body part.",
        "Hi Maxine! 🌿 Milo wants to check: is a bird living? Is a fish living? Let's find out what living things need!"),
    "science-g3-q2-w03-d03.json": ("Cat and Banana Plant",
        "Classify a cat and a banana plant as living things and describe a basic need or body part.",
        "Hi Maxine! 🌿 Milo wants to check: is a cat living? Is a banana plant living? Let's find out what living things need!"),
    "science-g3-q2-w03-d04.json": ("Ant and Duck",
        "Classify an ant and a duck as living things and describe a basic need or body part.",
        "Hi Maxine! 🌿 Milo wants to check: is an ant living? Is a duck living? Let's find out what living things need!"),
    "science-g3-q2-w03-d05.json": ("Butterfly and Cow",
        "Classify a butterfly and a cow as living things and describe a basic need or body part.",
        "Hi Maxine! 🌿 Milo wants to check: is a butterfly living? Is a cow living? Let's find out what living things need!"),
    "science-g3-q2-w04-d01.json": ("Frog and Bamboo",
        "Classify a frog and bamboo as living things and describe a basic need or body part.",
        "Hi Maxine! 🌿 Milo wants to check: is a frog living? Is bamboo living? Let's find out what living things need!"),
    "science-g3-q4-w08-d02.json": ("Horse and Rose",
        "Classify a horse and a rose as living things and describe a basic need or body part.",
        "Hi Maxine! 🌿 Milo wants to check: is a horse living? Is a rose living? Let's find out what living things need!"),
}

LIGHTSOUND = {
    "science-g3-q3-w05-d04.json": ("Guitar Strings Make Sound",
        "Describe how vibrating guitar strings make sound and how to protect our ears.",
        "Hi Maxine! 🎸 Milo wants to show you how a guitar makes sound. Let's see how sound behaves — and how to keep our ears safe!"),
    "science-g3-q3-w06-d01.json": ("Clapping Hands Make Sound",
        "Describe how clapping hands make sound and how to use sound safely.",
        "Hi Maxine! 👏 Milo wants to show you how clapping makes sound. Let's see how sound behaves — and how to use it safely!"),
    "science-g3-q4-w07-d02.json": ("The Drum's Sound",
        "Describe how a drum makes sound when struck and how to protect our ears.",
        "Hi Maxine! 🥁 Milo wants to show you how a drum makes sound. Let's see how sound behaves — and how to keep our ears safe!"),
    "science-g3-q4-w08-d01.json": ("Mirrors Reflect Light",
        "Describe how a mirror reflects light and how to protect our eyes.",
        "Hi Maxine! 🪞 Milo wants to show you how a mirror reflects light. Let's see how light behaves — and how to keep our eyes safe!"),
    "science-g3-q4-w08-d03.json": ("Shadows Block Light",
        "Describe how shadows form when light is blocked and how to protect our eyes.",
        "Hi Maxine! 🌑 Milo wants to show you how shadows form. Let's see how light behaves — and how to keep our eyes safe!"),
    "science-g3-q4-w08-d04.json": ("Windows Let Light Through",
        "Describe how windows let light through and how to protect our eyes.",
        "Hi Maxine! 🪟 Milo wants to show you how light passes through a window. Let's see how light behaves — and how to keep our eyes safe!"),
    "science-g3-q4-w09-d01.json": ("Rainbows Bend Light",
        "Describe how a rainbow appears when light bends and how to protect our eyes.",
        "Hi Maxine! 🌈 Milo wants to show you how a rainbow appears. Let's see how light behaves — and how to keep our eyes safe!"),
}

ROOTS = {
    "english-g3-q2-w02-d02.json": ("Roots: Play, Help, Teach",
        "Identify the base words play, help, and teach in common related words.",
        "Hi Maxine! 🌱 A base word is like a root. Today's roots: play, help, and teach. Let's see how new words grow from them!"),
    "english-g3-q2-w02-d03.json": ("Roots: Jump, Read, Walk",
        "Identify the base words jump, read, and walk in common related words.",
        "Hi Maxine! 🌱 A base word is like a root. Today's roots: jump, read, and walk. Let's see how new words grow from them!"),
    "english-g3-q2-w02-d04.json": ("Roots: Clean, Sing, Slow",
        "Identify the base words clean, sing, and slow in common related words.",
        "Hi Maxine! 🌱 A base word is like a root. Today's roots: clean, sing, and slow. Let's see how new words grow from them!"),
    "english-g3-q3-w09-d01.json": ("Roots: Look, Paint, Fast",
        "Identify the base words look, paint, and fast in common related words.",
        "Hi Maxine! 🌱 A base word is like a root. Today's roots: look, paint, and fast. Let's see how new words grow from them!"),
    "english-g3-q3-w09-d02.json": ("Roots: Work, Write, Quick",
        "Identify the base words work, write, and quick in common related words.",
        "Hi Maxine! 🌱 A base word is like a root. Today's roots: work, write, and quick. Let's see how new words grow from them!"),
    "english-g3-q3-w09-d03.json": ("Roots: Call, Build, Loud",
        "Identify the base words call, build, and loud in common related words.",
        "Hi Maxine! 🌱 A base word is like a root. Today's roots: call, build, and loud. Let's see how new words grow from them!"),
}

STORY = {
    "english-g3-q2-w05-d03.json": ("Milo's Story",
        "Use details from a story about Milo to explain characters, events, and ideas.",
        "Hi Maxine! 📖 Milo has a story about himself. Let's find the clues: who is in it, where it happens, and what Milo did!"),
    "english-g3-q2-w06-d02.json": ("Ana's Story",
        "Use details from a story about Ana to explain characters, events, and ideas.",
        "Hi Maxine! 📖 Milo has a story about Ana. Let's find the clues: who is in it, where it happens, and what Ana did!"),
    "english-g3-q2-w07-d02.json": ("Maxine's Story",
        "Use details from a story about Maxine to explain characters, events, and ideas.",
        "Hi Maxine! 📖 Milo has a story about YOU! Let's find the clues: who is in it, where it happens, and what Maxine did!"),
    "english-g3-q3-w12-d02.json": ("Ben's Story",
        "Use details from a story about Ben to explain characters, events, and ideas.",
        "Hi Maxine! 📖 Milo has a story about Ben. Let's find the clues: who is in it, where it happens, and what Ben did!"),
    "english-g3-q3-w14-d01.json": ("Nina's Story",
        "Use details from a story about Nina to explain characters, events, and ideas.",
        "Hi Maxine! 📖 Milo has a story about Nina. Let's find the clues: who is in it, where it happens, and what Nina did!"),
}

PILIPINO = {
    "makabansa-g3-q2-w02-d02.json": ("Bakit Mahalaga ang Pagkakakilanlan?",
        "Naipaliliwanag kung bakit mahalaga ang pagkakakilanlang Pilipino.",
        "Kumusta, Maxine! 🇵🇭 Bakit nga ba mahalaga ang ating pagkakakilanlan bilang Pilipino? Sama ka kay Milo at alamin natin!"),
    "makabansa-g3-q2-w02-d03.json": ("Maibabahagi Ko ang Pagka-Pilipino",
        "Naiuugnay ang sariling katangian sa pagbabahagi ng pagkakakilanlang Pilipino.",
        "Kumusta, Maxine! 🇵🇭 Ano ang maaari mong ibahagi tungkol sa pagiging Pilipino? Tara, tuklasin natin ang iyong sariling kuwento!"),
    "makabansa-g3-q3-w03-d01.json": ("Kilala Ko ang Aking Pagkakakilanlan",
        "Naipaliliwanag kung bakit kailangang malaman ng bawat bata ang kaniyang pagkakakilanlang Pilipino.",
        "Kumusta, Maxine! 🇵🇭 Bakit kailangan nating malaman kung sino tayo bilang Pilipino? Alamin natin kasama si Milo!"),
    "makabansa-g3-q3-w03-d02.json": ("Bahagi ng Pagkakakilanlan Ko",
        "Naiuugnay ang sariling karanasan sa mga bahagi ng pagkakakilanlang Pilipino.",
        "Kumusta, Maxine! 🇵🇭 Nakatira ka sa Pilipinas — kaya bahagi nito ang kuwento mo! Anong mga karanasan ang bahagi ng iyong pagkakakilanlan?"),
    "makabansa-g3-q3-w03-d03.json": ("Araw-araw na Pagpapakita ng Pagka-Pilipino",
        "Naipakikita ang pagiging Pilipino sa pang-araw-araw na kilos at gawain.",
        "Kumusta, Maxine! 🇵🇭 Hindi lang sa pista — sa araw-araw, may mga paraan tayo para ipakita ang pagiging Pilipino. Tara na!"),
}

SENTENCE = {
    "english-g3-q2-w03-d01.json": ("Blue and Shiny: Complete?",
        "Tell whether groups of words like 'blue and shiny' express a complete idea.",
        "Hi Maxine! 🧩 Milo found the words 'blue and shiny'. Is that a whole sentence? Let's find out what makes an idea complete!"),
    "english-g3-q2-w03-d02.json": ("Green and Leafy: Complete?",
        "Tell whether groups of words like 'green and leafy' express a complete idea.",
        "Hi Maxine! 🧩 Milo found the words 'green and leafy'. Is that a whole sentence? Let's find out what makes an idea complete!"),
    "english-g3-q3-w09-d04.json": ("Loud and Clear: Complete?",
        "Tell whether groups of words like 'loud and clear' express a complete idea.",
        "Hi Maxine! 🧩 Milo found the words 'loud and clear'. Is that a whole sentence? Let's find out what makes an idea complete!"),
    "english-g3-q3-w10-d01.json": ("Dark and Cold: Complete?",
        "Tell whether groups of words like 'dark and cold' express a complete idea.",
        "Hi Maxine! 🧩 Milo found the words 'dark and cold'. Is that a whole sentence? Let's find out what makes an idea complete!"),
}

CAPITALS = {
    "english-g3-q2-w04-d02.json": ("Capitals and Periods",
        "Use a capital letter and a period in a simple telling sentence.",
        "Hi Maxine! 🔤 Every telling sentence starts with a capital and ends with a period. Let's practice with Milo!"),
    "english-g3-q2-w04-d04.json": ("Telling and Asking Sentences",
        "Use a capital letter and the correct end mark in telling and asking sentences.",
        "Hi Maxine! 🔤 Telling sentences end with a period. Asking sentences end with a question mark. Let's try both!"),
    "english-g3-q3-w11-d01.json": ("Neighborhood Sentences",
        "Use a capital letter and correct end punctuation in neighborhood sentences.",
        "Hi Maxine! 🔤 Milo wrote sentences about the neighborhood. Let's check their capital letters and end marks!"),
    "english-g3-q3-w11-d03.json": ("Checking End Marks",
        "Reinforce using capital letters and end marks in simple sentences.",
        "Hi Maxine! 🔤 Let's practice again: every sentence needs a capital letter and the right end mark. Ready, reviewer?"),
}

SALITANGUGAT = {
    "filipino-g3-q1-w01-d05.json": ("Ugat ng 'Sumulat'",
        "Natutukoy ang salitang-ugat ng 'sumulat' at ng mga katulad na salita.",
        "Kumusta, Maxine! 🌱 Ang salitang 'sumulat' ay may ugat. Hanapin natin kung saan ito nagmula!"),
    "filipino-g3-q2-w05-d02.json": ("Ugat ng 'Kumain'",
        "Natutukoy ang salitang-ugat ng 'kumain' at ng mga katulad na salita.",
        "Kumusta, Maxine! 🌱 Ang salitang 'kumain' ay may ugat. Hanapin natin kung saan ito nagmula!"),
    "filipino-g3-q3-w09-d01.json": ("Ugat ng 'Natulog'",
        "Natutukoy ang salitang-ugat ng 'natulog' at ng mga katulad na salita.",
        "Kumusta, Maxine! 🌱 Ang salitang 'natulog' ay may ugat. Hanapin natin kung saan ito nagmula!"),
    "filipino-g3-q4-w12-d03.json": ("Ugat ng 'Nagluto'",
        "Natutukoy ang salitang-ugat ng 'nagluto' at ng mga katulad na salita.",
        "Kumusta, Maxine! 🌱 Ang salitang 'nagluto' ay may ugat. Hanapin natin kung saan ito nagmula!"),
}

MAGAGALANG = {
    "filipino-g3-q1-w02-d04.json": ("Magalang na Pagbati sa Umaga",
        "Nagagamit ang magagalang na pagbati at pananalita sa umaga.",
        "Kumusta, Maxine! 🌞 Tuwing umaga, may mga salitang magagalang tayong ginagamit. Tara, aralin natin ang mga ito!"),
    "filipino-g3-q2-w06-d02.json": ("Magalang na Pagbati sa Hapon",
        "Nagagamit ang magagalang na pagbati at pananalita sa hapon.",
        "Kumusta, Maxine! 🌤️ Tuwing hapon, may mga salitang magagalang tayong ginagamit. Tara, aralin natin ang mga ito!"),
    "filipino-g3-q3-w09-d05.json": ("Magalang na Pagbati sa Gabi",
        "Nagagamit ang magagalang na pagbati at pananalita sa gabi.",
        "Kumusta, Maxine! 🌙 Tuwing gabi, may mga salitang magagalang tayong ginagamit. Tara, aralin natin ang mga ito!"),
    "filipino-g3-q4-w13-d03.json": ("Magalang na Pagbati sa Lahat ng Oras",
        "Nagagamit ang magagalang na pagbati at pananalita sa iba't ibang oras at sitwasyon.",
        "Kumusta, Maxine! 🙏 Umaga man, hapon, o gabi — may angkop na magagalang na pananalita. Sama ka kay Milo!"),
}

BUOD = {
    "filipino-g3-q1-w03-d04.json": ("Buod: Ang Alampay ni Lola Rosa",
        "Nakabubuo ng maikling buod ng kuwentong 'Ang Alampay ni Lola Rosa'.",
        "Kumusta, Maxine! 📚 May kuwento si Milo tungkol sa alampay ni Lola Rosa. Pagkatapos nating basahin, gagawa tayo ng maikling buod!"),
    "filipino-g3-q2-w07-d03.json": ("Buod: Si Ben at ang Mainit na Araw",
        "Nakabubuo ng maikling buod ng kuwentong 'Si Ben at ang Mainit na Araw'.",
        "Kumusta, Maxine! 📚 May kuwento si Milo tungkol kay Ben na gustong maglaro. Pagkatapos nating basahin, gagawa tayo ng maikling buod!"),
    "filipino-g3-q3-w10-d04.json": ("Buod: Ang Paso ni Aling Rosa",
        "Nakabubuo ng maikling buod ng kuwentong 'Ang Paso ni Aling Rosa'.",
        "Kumusta, Maxine! 📚 May kuwento si Milo tungkol sa paso ni Aling Rosa. Pagkatapos nating basahin, gagawa tayo ng maikling buod!"),
    "filipino-g3-q4-w14-d04.json": ("Buod: Ang Jacket ni Marco",
        "Nakabubuo ng maikling buod ng kuwentong 'Ang Jacket ni Marco'.",
        "Kumusta, Maxine! 📚 May kuwento si Milo tungkol kay Marco at sa ulan. Pagkatapos nating basahin, gagawa tayo ng maikling buod!"),
}

TALATA = {
    "filipino-g3-q1-w03-d01.json": ("Talata: Ang Aking Pusa",
        "Nakabubuo ng maikling talata tungkol sa aking pusa na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol sa pusa. Hanapin natin ang paksa at mga detalye nito!"),
    "filipino-g3-q1-w03-d03.json": ("Talata: Sabado ng Pamilya ni Ana",
        "Nakabubuo ng maikling talata tungkol sa Sabado ng pamilya ni Ana na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol sa Sabado ng pamilya ni Ana. Hanapin natin ang paksa at mga detalye nito!"),
    "filipino-g3-q1-w03-d05.json": ("Talata: Ang Paaralan ni Ben",
        "Nakabubuo ng maikling talata tungkol sa paaralan ni Ben na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol sa paaralan ni Ben. Hanapin natin ang paksa at mga detalye nito!"),
    "filipino-g3-q2-w07-d01.json": ("Talata: Ang Maulan na Linggo",
        "Nakabubuo ng maikling talata tungkol sa maulan na Linggo na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol sa maulan na Linggo. Hanapin natin ang paksa at mga detalye nito!"),
    "filipino-g3-q2-w07-d02.json": ("Talata: Ang Hardin ni Tito Ramon",
        "Nakabubuo ng maikling talata tungkol sa hardin ni Tito Ramon na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol sa hardin ni Tito Ramon. Hanapin natin ang paksa at mga detalye nito!"),
    "filipino-g3-q2-w07-d04.json": ("Talata: Ang Maingat na si Dina",
        "Nakabubuo ng maikling talata tungkol sa maingat na si Dina na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol sa maingat na si Dina. Hanapin natin ang paksa at mga detalye nito!"),
    "filipino-g3-q3-w10-d02.json": ("Talata: Basketbol sa Plasa",
        "Nakabubuo ng maikling talata tungkol sa laro ng basketbol sa plasa na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol sa basketbol sa plasa. Hanapin natin ang paksa at mga detalye nito!"),
    "filipino-g3-q3-w10-d03.json": ("Talata: Ang Aming Barangay",
        "Nakabubuo ng maikling talata tungkol sa aming barangay na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol sa barangay. Hanapin natin ang paksa at mga detalye nito!"),
    "filipino-g3-q3-w10-d05.json": ("Talata: Ang Aso ni Milo",
        "Nakabubuo ng maikling talata tungkol sa aso ni Milo na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol sa aso ni Milo. Hanapin natin ang paksa at mga detalye nito!"),
    "filipino-g3-q4-w14-d02.json": ("Talata: Si Lola Nena",
        "Nakabubuo ng maikling talata tungkol kay Lola Nena na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol kay Lola Nena. Hanapin natin ang paksa at mga detalye nito!"),
    "filipino-g3-q4-w14-d03.json": ("Talata: Ang Palengke",
        "Nakabubuo ng maikling talata tungkol sa palengke na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol sa palengke. Hanapin natin ang paksa at mga detalye nito!"),
    "filipino-g3-q4-w15-d01.json": ("Talata: Si Jose",
        "Nakabubuo ng maikling talata tungkol kay Jose na malinaw ang paksa at mga detalye.",
        "Kumusta, Maxine! 📝 May talata tayong aaralin tungkol kay Jose. Hanapin natin ang paksa at mga detalye nito!"),
}

ALL_GROUPS = {
    "Naipaliliwanag kung paano nakaaapekto ang kapaligiran sa kultura ng komunidad.": KAPALIGIRAN,
    "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa.": PAGGALANG,
    "Describe familiar materials by observable properties and choose safe uses or handling.": MATERIALS,
    "Add numbers with sums up to 10,000, with or without regrouping.": ADDITION,
    "Multiply numbers by using place value, groups, and an accurate algorithm.": MULTIPLY,
    "Use high-frequency and content-specific words in context.": WORDS,
    "Naisusulat nang maayos at wasto ang mga natutuhang salita.": WASTONG,
    "Classify familiar examples as living or non-living and describe basic needs or body parts.": LIVING,
    "Describe how light and sound behave and identify safe ways to protect people.": LIGHTSOUND,
    "Identify a base word, or root, in common related words.": ROOTS,
    "Use details from a story to explain characters, events, and ideas.": STORY,
    "Naiuugnay ang sariling katangian at karanasan sa pagiging Pilipino.": PILIPINO,
    "Tell whether a group of words expresses a complete idea.": SENTENCE,
    "Use a capital letter and correct end punctuation in a simple sentence.": CAPITALS,
    "Natutukoy ang salitang-ugat ng mga karaniwang salita.": SALITANGUGAT,
    "Nagagamit ang magagalang na pagbati at pananalita ayon sa sitwasyon.": MAGAGALANG,
    "Nakabubuo ng maikling buod ng tekstong naratibo.": BUOD,
    "Nakabubuo ng maikling talata na malinaw ang paksa at mga detalye.": TALATA,
}


def main() -> int:
    changed = 0
    missing = 0
    for name in sorted(os.listdir(ROOT)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(ROOT, name)
        with open(path, encoding="utf-8") as fh:
            lesson = json.load(fh)
        objective = (lesson.get("objective") or "").strip()
        group = ALL_GROUPS.get(objective)
        if group is None:
            continue
        meta = group.get(name)
        if meta is None:
            print(f"  WARN {name}: in group but no meta entry", file=sys.stderr)
            missing += 1
            continue
        title, new_objective, intro = meta
        lesson["title"] = title
        lesson["objective"] = new_objective
        lesson["introduction"] = intro
        lesson["pacingPass"] = MARKER
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(lesson, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
        changed += 1

    print(f"B2 applied to {changed} lessons, {missing} missing meta entries")
    return 0


if __name__ == "__main__":
    sys.exit(main())
