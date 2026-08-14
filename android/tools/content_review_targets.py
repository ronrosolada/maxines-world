"""Stable lesson groups covered by historical content-repair tests.

These IDs are the reviewed lesson cohorts, not runtime classification rules.
Keeping the manifest explicit prevents an educator-authoring pass from being
mistaken for a missing repair simply because the child-facing objective was
rewritten.
"""

FILIPINO_GROUPS = {
    "simuno": (
        "filipino-g3-q1-w01-d01", "filipino-g3-q1-w01-d02", "filipino-g3-q1-w01-d03",
        "filipino-g3-q1-w02-d01", "filipino-g3-q1-w02-d02", "filipino-g3-q1-w02-d03",
        "filipino-g3-q1-w02-d05", "filipino-g3-q1-w03-d02", "filipino-g3-q2-w04-d02",
        "filipino-g3-q2-w04-d03", "filipino-g3-q2-w04-d04", "filipino-g3-q2-w05-d03",
        "filipino-g3-q2-w05-d04", "filipino-g3-q2-w06-d01", "filipino-g3-q2-w06-d03",
        "filipino-g3-q2-w06-d04", "filipino-g3-q3-w08-d02", "filipino-g3-q3-w08-d03",
        "filipino-g3-q3-w08-d04", "filipino-g3-q3-w09-d02", "filipino-g3-q3-w09-d03",
        "filipino-g3-q3-w09-d04", "filipino-g3-q3-w10-d01", "filipino-g3-q4-w11-d01",
        "filipino-g3-q4-w11-d03", "filipino-g3-q4-w11-d04", "filipino-g3-q4-w12-d01",
        "filipino-g3-q4-w12-d04", "filipino-g3-q4-w13-d01", "filipino-g3-q4-w13-d02",
        "filipino-g3-q4-w13-d04", "filipino-g3-q4-w14-d01",
    ),
    "pagsulat": (
        "filipino-g3-q1-w01-d04", "filipino-g3-q2-w04-d01", "filipino-g3-q2-w05-d01",
        "filipino-g3-q3-w08-d01", "filipino-g3-q3-w08-d05", "filipino-g3-q4-w11-d02",
        "filipino-g3-q4-w12-d02",
    ),
    "salitangugat": (
        "filipino-g3-q1-w01-d05", "filipino-g3-q2-w05-d02", "filipino-g3-q3-w09-d01",
        "filipino-g3-q4-w12-d03",
    ),
    "pananalita": (
        "filipino-g3-q1-w02-d04", "filipino-g3-q2-w06-d02", "filipino-g3-q3-w09-d05",
        "filipino-g3-q4-w13-d03",
    ),
    "talata": (
        "filipino-g3-q1-w03-d01", "filipino-g3-q1-w03-d03", "filipino-g3-q1-w03-d05",
        "filipino-g3-q2-w07-d01", "filipino-g3-q2-w07-d02", "filipino-g3-q2-w07-d04",
        "filipino-g3-q3-w10-d02", "filipino-g3-q3-w10-d03", "filipino-g3-q3-w10-d05",
        "filipino-g3-q4-w14-d02", "filipino-g3-q4-w14-d03", "filipino-g3-q4-w15-d01",
    ),
    "buod": (
        "filipino-g3-q1-w03-d04", "filipino-g3-q2-w07-d03", "filipino-g3-q3-w10-d04",
        "filipino-g3-q4-w14-d04",
    ),
}

MATH_SCIENCE_GROUPS = {
    "math-add": (
        "mathematics-g3-q2-w03-d02", "mathematics-g3-q2-w03-d03", "mathematics-g3-q2-w03-d04",
        "mathematics-g3-q2-w03-d05", "mathematics-g3-q2-w04-d01", "mathematics-g3-q2-w04-d04",
        "mathematics-g3-q2-w04-d05", "mathematics-g3-q3-w06-d04", "mathematics-g3-q4-w08-d04",
    ),
    "math-mul": (
        "mathematics-g3-q3-w05-d04", "mathematics-g3-q3-w06-d01", "mathematics-g3-q3-w06-d03",
        "mathematics-g3-q3-w07-d01", "mathematics-g3-q3-w07-d02", "mathematics-g3-q4-w08-d01",
        "mathematics-g3-q4-w08-d02", "mathematics-g3-q4-w08-d03", "mathematics-g3-q4-w09-d01",
    ),
    "sci-living": (
        "science-g3-q1-w01-d01", "science-g3-q2-w03-d02", "science-g3-q2-w03-d03",
        "science-g3-q2-w03-d04", "science-g3-q2-w03-d05", "science-g3-q2-w04-d01",
        "science-g3-q4-w08-d02",
    ),
    "sci-materials": (
        "science-g3-q1-w01-d02", "science-g3-q1-w01-d03", "science-g3-q1-w01-d04",
        "science-g3-q1-w01-d05", "science-g3-q1-w02-d01", "science-g3-q1-w02-d02",
        "science-g3-q3-w05-d02", "science-g3-q3-w05-d03", "science-g3-q3-w06-d02",
        "science-g3-q4-w07-d03", "science-g3-q4-w07-d04",
    ),
    "sci-light": (
        "science-g3-q3-w05-d04", "science-g3-q3-w06-d01", "science-g3-q4-w07-d02",
        "science-g3-q4-w08-d01", "science-g3-q4-w08-d03", "science-g3-q4-w08-d04",
        "science-g3-q4-w09-d01",
    ),
}

ENGLISH_GROUPS = {
    "word": (
        "english-g3-q2-w01-d01", "english-g3-q2-w01-d02", "english-g3-q2-w01-d03",
        "english-g3-q2-w01-d04", "english-g3-q3-w08-d01", "english-g3-q3-w08-d02",
        "english-g3-q3-w08-d03",
    ),
    "root": (
        "english-g3-q2-w02-d02", "english-g3-q2-w02-d03", "english-g3-q2-w02-d04",
        "english-g3-q3-w09-d01", "english-g3-q3-w09-d02", "english-g3-q3-w09-d03",
    ),
    "complete": (
        "english-g3-q2-w03-d01", "english-g3-q2-w03-d02", "english-g3-q3-w09-d04",
        "english-g3-q3-w10-d01",
    ),
    "story": (
        "english-g3-q2-w05-d03", "english-g3-q2-w06-d02", "english-g3-q2-w07-d02",
        "english-g3-q3-w12-d02", "english-g3-q3-w14-d01",
    ),
}

ALL_REVIEW_GROUPS = {**FILIPINO_GROUPS, **MATH_SCIENCE_GROUPS, **ENGLISH_GROUPS}
