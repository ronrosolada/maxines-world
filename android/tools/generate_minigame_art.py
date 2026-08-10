#!/usr/bin/env python3
"""Generate cohesive, offline mini-game thumbnails for the reward-break library."""
from __future__ import annotations

import hashlib
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/res/drawable-nodpi"
SIZE = (512, 288)

PALETTES = {
    "2048": ((34, 64, 103), (82, 141, 194), (255, 205, 91), (255, 239, 177)),
    "bolt-sort": ((38, 76, 70), (93, 166, 125), (255, 211, 89), (255, 245, 198)),
    "breakout": ((70, 45, 108), (177, 88, 151), (255, 121, 108), (255, 227, 117)),
    "checkers": ((80, 54, 45), (177, 112, 69), (244, 124, 107), (255, 226, 170)),
    "color-block": ((48, 75, 113), (97, 154, 201), (255, 174, 76), (255, 236, 161)),
    "color-connect": ((39, 77, 92), (56, 155, 166), (255, 194, 83), (255, 235, 166)),
    "connect-four": ((32, 74, 128), (60, 143, 205), (255, 207, 76), (255, 238, 165)),
    "domino": ((91, 66, 48), (204, 142, 78), (255, 190, 88), (255, 239, 182)),
    "flappy-bird": ((40, 110, 148), (127, 205, 217), (255, 197, 81), (255, 245, 188)),
    "freecell": ((28, 91, 91), (70, 160, 139), (255, 220, 112), (255, 245, 195)),
    "mancala": ((98, 63, 48), (205, 132, 73), (242, 117, 100), (255, 227, 172)),
    "match-three": ((93, 52, 104), (212, 109, 159), (255, 218, 101), (255, 239, 190)),
    "memory-match": ((39, 86, 108), (84, 169, 184), (255, 164, 111), (255, 239, 187)),
    "number-merge": ((39, 66, 112), (93, 141, 202), (255, 190, 80), (255, 239, 180)),
    "onet-connect": ((66, 61, 115), (126, 121, 202), (255, 205, 91), (255, 239, 179)),
    "piano-tiles": ((42, 46, 76), (91, 110, 175), (255, 126, 153), (255, 215, 231)),
    "pong": ((31, 82, 86), (74, 163, 144), (255, 207, 88), (255, 241, 180)),
    "reversi": ((29, 83, 75), (67, 157, 117), (255, 186, 88), (255, 237, 182)),
    "sliding-puzzle": ((57, 75, 111), (120, 158, 203), (255, 178, 101), (255, 235, 185)),
    "snake": ((43, 91, 65), (91, 175, 103), (255, 213, 92), (255, 245, 187)),
    "solitaire": ((37, 93, 84), (72, 165, 139), (244, 124, 107), (255, 231, 184)),
    "stack": ((80, 58, 105), (151, 104, 177), (255, 190, 83), (255, 237, 179)),
    "sudoku": ((36, 72, 111), (82, 145, 194), (255, 185, 90), (255, 240, 188)),
    "tetris": ((74, 49, 106), (155, 87, 177), (102, 224, 198), (238, 255, 223)),
    "tictactoe": ((46, 86, 101), (93, 165, 178), (255, 153, 115), (255, 238, 186)),
    "whack-a-mole": ((71, 101, 57), (158, 190, 93), (255, 190, 89), (255, 240, 177)),
    "word-search": ((58, 76, 107), (109, 146, 190), (255, 188, 100), (255, 239, 185)),
    "wordle": ((50, 92, 87), (94, 168, 127), (255, 208, 87), (255, 242, 184)),
    "yahtzee": ((75, 64, 102), (139, 113, 177), (255, 181, 99), (255, 239, 187)),
}

TILE_GAMES = {"2048", "color-block", "match-three", "number-merge", "onet-connect", "sliding-puzzle", "tetris"}
BOARD_GAMES = {"checkers", "connect-four", "domino", "freecell", "mancala", "memory-match", "reversi", "solitaire", "sudoku", "tictactoe", "yahtzee"}
ARCADE_GAMES = {"breakout", "flappy-bird", "piano-tiles", "pong", "snake", "stack", "whack-a-mole"}


def mix(a, b, t):
    return tuple(round(a[i] * (1 - t) + b[i] * t) for i in range(3))


def rounded(draw, box, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def shadowed_round(draw, box, radius, fill, shadow=(8, 10, 20, 60), outline=None):
    x1, y1, x2, y2 = box
    rounded(draw, (x1 + 6, y1 + 8, x2 + 6, y2 + 8), radius, shadow)
    rounded(draw, box, radius, fill, outline=outline, width=2 if outline else 1)


def paw(draw, x, y, scale, fill):
    draw.ellipse((x - 16 * scale, y - 7 * scale, x + 16 * scale, y + 21 * scale), fill=fill)
    for dx, dy, r in [(-18, -17, 7), (-6, -24, 7), (7, -23, 7), (18, -14, 6)]:
        draw.ellipse((x + (dx - r) * scale, y + (dy - r) * scale,
                      x + (dx + r) * scale, y + (dy + r) * scale), fill=fill)


def buddy(draw, cx, cy, scale, fur, ear, eye=(25, 37, 45)):
    # A tiny consistent animal friend gives the library a Maxine's World signature.
    r = 42 * scale
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=fur)
    draw.polygon([(cx - r * .72, cy - r * .45), (cx - r * 1.12, cy - r * 1.25), (cx - r * .20, cy - r * .82)], fill=ear)
    draw.polygon([(cx + r * .72, cy - r * .45), (cx + r * 1.12, cy - r * 1.25), (cx + r * .20, cy - r * .82)], fill=ear)
    eye_r = 5 * scale
    for dx in (-15, 15):
        draw.ellipse((cx + dx * scale - eye_r, cy - 7 * scale - eye_r,
                      cx + dx * scale + eye_r, cy - 7 * scale + eye_r), fill=eye)
        draw.ellipse((cx + dx * scale - eye_r / 3, cy - 9 * scale - eye_r / 3,
                      cx + dx * scale + eye_r / 3, cy - 9 * scale + eye_r / 3), fill="white")
    draw.arc((cx - 13 * scale, cy + 1 * scale, cx + 13 * scale, cy + 19 * scale), 15, 165, fill=eye, width=max(1, round(3 * scale)))
    draw.ellipse((cx - 4 * scale, cy + 1 * scale, cx + 4 * scale, cy + 7 * scale), fill=(231, 117, 108))


def draw_tiles(draw, rng, accent, light, dark):
    x0, y0, size, gap = 132, 42, 52, 9
    colors = [accent, light, (111, 212, 185), (244, 124, 107), (255, 231, 133)]
    for row in range(4):
        for col in range(4):
            x = x0 + col * (size + gap)
            y = y0 + row * (size + gap)
            fill = colors[(row * 3 + col + rng.randrange(2)) % len(colors)]
            shadowed_round(draw, (x, y, x + size, y + size), 12, fill, shadow=(18, 27, 53, 70))
            if (row + col) % 3 == 0:
                draw.ellipse((x + 18, y + 18, x + 34, y + 34), fill=light)
    draw.polygon([(105, 80), (121, 65), (121, 75), (144, 75), (144, 85), (121, 85), (121, 95)], fill=light)


def draw_card(draw, box, fill, light, dark, symbol=None):
    shadowed_round(draw, box, 12, fill, shadow=(12, 23, 31, 75), outline=light)
    x1, y1, x2, y2 = box
    if symbol == "heart":
        draw.ellipse((x1 + 17, y1 + 22, x1 + 40, y1 + 45), fill=dark)
        draw.ellipse((x1 + 33, y1 + 22, x1 + 56, y1 + 45), fill=dark)
        draw.polygon([(x1 + 15, y1 + 35), (x1 + 49, y1 + 67), (x1 + 65, y1 + 35)], fill=dark)
    elif symbol == "paw":
        paw(draw, (x1 + x2) / 2, y1 + 39, .45, dark)


def draw_board(draw, rng, accent, light, dark, slug):
    board = mix(dark, (20, 35, 49), .35)
    if slug == "checkers":
        shadowed_round(draw, (100, 34, 412, 254), 24, (95, 53, 44), shadow=(16, 24, 28, 80), outline=light)
        for row in range(8):
            for col in range(8):
                fill = (241, 195, 126) if (row + col) % 2 == 0 else (119, 75, 58)
                draw.rectangle((121 + col * 34, 52 + row * 22, 155 + col * 34, 74 + row * 22), fill=fill)
                if (row + col) % 2 == 1 and row in (1, 2, 5, 6):
                    piece = accent if row < 4 else light
                    draw.ellipse((128 + col * 34, 54 + row * 22, 149 + col * 34, 75 + row * 22), fill=piece, outline=dark, width=2)
        return
    if slug == "connect-four":
        shadowed_round(draw, (100, 34, 412, 254), 26, (34, 102, 174), shadow=(14, 27, 57, 90), outline=light)
        for row in range(6):
            for col in range(7):
                fill = accent if (row + col) % 4 == 0 else light if (row * 2 + col) % 5 == 0 else (20, 61, 126)
                draw.ellipse((122 + col * 39, 51 + row * 31, 150 + col * 39, 79 + row * 31), fill=fill, outline=(18, 55, 112), width=3)
        return
    if slug == "domino":
        shadowed_round(draw, (104, 42, 408, 246), 28, board, shadow=(16, 24, 28, 80), outline=light)
        draw_card(draw, (145, 70, 245, 218), light, accent, dark)
        draw.line((151, 144, 239, 144), fill=dark, width=4)
        draw.ellipse((174, 94, 188, 108), fill=dark)
        draw.ellipse((202, 177, 216, 191), fill=dark)
        draw_card(draw, (267, 70, 367, 218), accent, light, dark)
        draw.line((273, 144, 361, 144), fill=dark, width=4)
        for x, y in [(293, 93), (333, 93), (293, 177), (333, 177)]:
            draw.ellipse((x, y, x + 14, y + 14), fill=dark)
        return
    if slug in {"freecell", "solitaire"}:
        shadowed_round(draw, (100, 36, 412, 252), 28, (26, 92, 84), shadow=(16, 24, 28, 80), outline=light)
        draw_card(draw, (126, 66, 188, 163), light, accent, dark, "heart")
        draw_card(draw, (154, 82, 216, 179), accent, light, dark)
        draw_card(draw, (184, 98, 246, 195), light, accent, dark)
        for x in (286, 337, 388):
            draw.ellipse((x - 15, 71, x + 15, 101), outline=light, width=4)
        draw.line((267, 179, 404, 179), fill=light, width=5)
        return
    if slug == "mancala":
        shadowed_round(draw, (90, 62, 422, 226), 80, (130, 78, 49), shadow=(16, 24, 28, 80), outline=light)
        for x in [126, 173, 220, 267, 314, 361]:
            for y in (108, 178):
                draw.ellipse((x - 17, y - 17, x + 17, y + 17), fill=(77, 43, 36), outline=light, width=2)
                for j in range(3):
                    draw.ellipse((x - 7 + j * 5, y - 4, x - 1 + j * 5, y + 2), fill=[accent, light, (111, 212, 185)][(j + x) % 3])
        return
    if slug == "memory-match":
        shadowed_round(draw, (104, 36, 408, 252), 28, board, shadow=(16, 24, 28, 80), outline=light)
        for row in range(3):
            for col in range(4):
                x, y = 132 + col * 62, 56 + row * 61
                fill = accent if (row + col) % 3 == 0 else light if (row + col) % 3 == 1 else (111, 212, 185)
                draw_card(draw, (x, y, x + 48, y + 48), fill if (row + col) % 2 else dark, light, dark, "paw" if (row + col) % 2 == 0 else None)
        return
    if slug == "reversi":
        shadowed_round(draw, (104, 36, 408, 252), 28, (31, 111, 76), shadow=(16, 24, 28, 80), outline=light)
        for i in range(1, 8):
            draw.line((116 + i * 36, 48, 116 + i * 36, 240), fill=(158, 214, 145), width=2)
            draw.line((116, 48 + i * 27, 396, 48 + i * 27), fill=(158, 214, 145), width=2)
        for x, y, fill in [(188, 112, dark), (224, 139, light), (260, 112, light), (260, 166, dark), (296, 139, dark)]:
            draw.ellipse((x - 16, y - 16, x + 16, y + 16), fill=fill, outline=(255, 255, 255, 130), width=2)
        return
    if slug == "sudoku":
        shadowed_round(draw, (107, 38, 405, 250), 24, light, shadow=(16, 24, 28, 80), outline=accent)
        for i in range(10):
            width = 4 if i % 3 == 0 else 1
            draw.line((126 + i * 32, 54, 126 + i * 32, 234), fill=dark, width=width)
            draw.line((126, 54 + i * 20, 382, 54 + i * 20), fill=dark, width=width)
        for x, y in [(142, 66), (206, 86), (270, 66), (334, 106), (174, 146), (302, 186), (238, 206)]:
            draw.ellipse((x - 5, y - 5, x + 5, y + 5), fill=accent)
        return
    if slug == "tictactoe":
        shadowed_round(draw, (108, 38, 404, 250), 26, board, shadow=(16, 24, 28, 80), outline=light)
        for x in (206, 306):
            draw.line((x, 70, x, 218), fill=light, width=8)
        for y in (120, 170):
            draw.line((154, y, 358, y), fill=light, width=8)
        draw.ellipse((176, 82, 218, 124), outline=accent, width=8)
        draw.line((254, 130, 286, 162), fill=light, width=8)
        draw.line((286, 130, 254, 162), fill=light, width=8)
        draw.ellipse((320, 180, 362, 222), outline=accent, width=8)
        return
    if slug == "yahtzee":
        shadowed_round(draw, (100, 38, 412, 250), 28, board, shadow=(16, 24, 28, 80), outline=light)
        for box, fill, dots in [((126, 86, 216, 176), light, [(45, 45)]), ((211, 55, 301, 145), accent, [(25, 25), (65, 65)]), ((296, 104, 386, 194), light, [(25, 25), (65, 65), (25, 65), (65, 25)])]:
            draw_card(draw, box, fill, accent, dark)
            x1, y1, x2, y2 = box
            for dx, dy in dots:
                draw.ellipse((x1 + dx - 7, y1 + dy - 7, x1 + dx + 7, y1 + dy + 7), fill=dark)
        return
    shadowed_round(draw, (104, 36, 408, 252), 28, board, shadow=(16, 24, 28, 80), outline=light)
    for row in range(4):
        for col in range(5):
            x, y = 137 + col * 55, 63 + row * 42
            if rng.random() < .78:
                fill = accent if (row + col) % 3 else light
                draw.ellipse((x - 14, y - 14, x + 14, y + 14), fill=(15, 35, 43), outline=light, width=2)
                draw.ellipse((x - 10, y - 12, x + 10, y + 8), fill=fill)
    draw.line((131, 210, 383, 210), fill=light, width=4)


def draw_arcade(draw, rng, accent, light, dark, slug):
    if slug == "breakout":
        shadowed_round(draw, (82, 36, 430, 252), 30, (28, 38, 70), shadow=(10, 12, 28, 90), outline=light)
        brick_colors = [accent, light, (111, 212, 185), (244, 124, 107)]
        for row in range(3):
            for col in range(7):
                fill = brick_colors[(row + col) % len(brick_colors)]
                rounded(draw, (106 + col * 41, 63 + row * 29, 138 + col * 41, 84 + row * 29), 6, fill)
        draw.line((190, 211, 322, 211), fill=light, width=13)
        draw.ellipse((274, 148, 308, 182), fill=accent, outline=light, width=3)
        draw.line((291, 165, 340, 204), fill=light, width=4)
        return
    if slug == "flappy-bird":
        shadowed_round(draw, (82, 36, 430, 252), 30, (111, 188, 206), shadow=(10, 12, 28, 90), outline=light)
        draw.ellipse((117, 60, 190, 91), fill=(255, 255, 255, 80))
        for x, gap in [(148, 108), (342, 156)]:
            draw.rectangle((x, 48, x + 43, gap), fill=(72, 157, 91), outline=light, width=3)
            draw.rectangle((x, gap + 65, x + 43, 240), fill=(72, 157, 91), outline=light, width=3)
            draw.rectangle((x - 8, gap - 8, x + 51, gap + 5), fill=(92, 180, 105), outline=light, width=2)
        draw.ellipse((235, 118, 281, 164), fill=accent, outline=light, width=3)
        draw.polygon([(238, 143), (211, 153), (238, 160)], fill=light)
        draw.ellipse((264, 128, 271, 135), fill=dark)
        return
    if slug == "piano-tiles":
        shadowed_round(draw, (82, 36, 430, 252), 30, (30, 36, 68), shadow=(10, 12, 28, 90), outline=light)
        for i in range(4):
            x = 112 + i * 76
            draw.line((x, 55, x, 237), fill=(155, 172, 211), width=2)
            draw.rectangle((x + 8, 72 + (i % 2) * 46, x + 59, 120 + (i % 2) * 46), fill=accent if i % 2 else light)
        draw.line((104, 201, 408, 201), fill=light, width=5)
        return
    if slug == "pong":
        shadowed_round(draw, (82, 36, 430, 252), 30, (22, 51, 63), shadow=(10, 12, 28, 90), outline=light)
        for y in range(53, 238, 24):
            draw.line((255, y, 255, y + 12), fill=light, width=5)
        draw.line((118, 105, 118, 178), fill=accent, width=13)
        draw.line((394, 135, 394, 208), fill=light, width=13)
        draw.ellipse((286, 94, 326, 134), fill=accent, outline=light, width=3)
        draw.line((306, 114, 365, 149), fill=light, width=4)
        return
    if slug == "snake":
        shadowed_round(draw, (82, 36, 430, 252), 30, (26, 70, 59), shadow=(10, 12, 28, 90), outline=light)
        for x in range(108, 408, 38):
            draw.line((x, 55, x, 233), fill=(105, 177, 109, 70), width=1)
        for y in range(55, 234, 36):
            draw.line((105, y, 408, y), fill=(105, 177, 109, 70), width=1)
        points = [(146, 80), (184, 80), (222, 80), (222, 116), (260, 116), (260, 152), (298, 152), (298, 188)]
        draw.line(points, fill=accent, width=24, joint="curve")
        draw.ellipse((134, 68, 158, 92), fill=light)
        draw.ellipse((139, 72, 144, 77), fill=dark)
        draw.ellipse((348, 78, 384, 114), fill=(244, 124, 107), outline=light, width=3)
        return
    if slug == "stack":
        shadowed_round(draw, (82, 36, 430, 252), 30, (52, 40, 74), shadow=(10, 12, 28, 90), outline=light)
        blocks = [(181, 204, 336, 232, accent), (201, 175, 350, 203, light), (159, 146, 304, 174, (111, 212, 185)), (212, 117, 349, 145, (244, 124, 107)), (239, 88, 347, 116, accent)]
        for x1, y1, x2, y2, fill in blocks:
            shadowed_round(draw, (x1, y1, x2, y2), 7, fill, shadow=(12, 15, 30, 70))
        draw.line((108, 235, 402, 235), fill=light, width=5)
        return
    if slug == "whack-a-mole":
        shadowed_round(draw, (82, 36, 430, 252), 30, (71, 106, 63), shadow=(10, 12, 28, 90), outline=light)
        for x, y in [(150, 111), (255, 164), (355, 105)]:
            draw.ellipse((x - 42, y + 20, x + 42, y + 49), fill=(39, 57, 44), outline=light, width=3)
            # Friendly hedgehog peeking from the hole.
            spikes = [(x - 36, y + 25), (x - 30, y - 4), (x - 14, y + 11), (x, y - 10), (x + 14, y + 11), (x + 30, y - 4), (x + 36, y + 25)]
            draw.polygon(spikes, fill=accent)
            draw.ellipse((x - 31, y + 2, x + 31, y + 54), fill=light, outline=accent, width=3)
            draw.ellipse((x - 16, y + 20, x - 9, y + 27), fill=dark)
            draw.ellipse((x + 9, y + 20, x + 16, y + 27), fill=dark)
            draw.ellipse((x - 5, y + 33, x + 5, y + 40), fill=(244, 124, 107))
        return
    shadowed_round(draw, (82, 36, 430, 252), 30, (22, 38, 62), shadow=(10, 12, 28, 90), outline=light)
    for i in range(8):
        x = 112 + i * 38
        y = 72 + int(math.sin(i * .9) * 23)
        draw.ellipse((x - 5, y - 5, x + 5, y + 5), fill=light)
    draw.line((122, 197, 390, 197), fill=accent, width=12)
    draw.line((122, 193, 390, 193), fill=light, width=3)
    bx, by = 284, 134
    draw.ellipse((bx - 18, by - 18, bx + 18, by + 18), fill=accent, outline=light, width=3)
    draw.line((bx - 18, by + 18, bx - 72, by + 62), fill=light, width=4)
    draw.line((bx + 18, by + 18, bx + 72, by + 62), fill=light, width=4)


def draw_words(draw, rng, accent, light, dark, slug):
    letters = "PLAY" if slug == "word-search" else "WORD"
    x0, y0, size, gap = 101, 78, 68, 12
    for i, letter in enumerate(letters):
        x = x0 + i * (size + gap)
        fill = accent if i in (0, 3) else light
        shadowed_round(draw, (x, y0, x + size, y0 + size), 14, fill, shadow=(21, 30, 51, 80))
        draw.text((x + 18, y0 + 8), letter, fill=dark, font=FONT)
    for _ in range(6):
        x = rng.randrange(92, 420)
        y = rng.randrange(40, 245)
        draw.ellipse((x, y, x + 5, y + 5), fill=light)


def render(slug):
    c1, c2, accent, light = PALETTES[slug]
    seed = int(hashlib.sha256(slug.encode()).hexdigest()[:8], 16)
    rng = random.Random(seed)
    image = Image.new("RGB", SIZE)
    px = image.load()
    for y in range(SIZE[1]):
        for x in range(SIZE[0]):
            t = (x / SIZE[0]) * .65 + (y / SIZE[1]) * .35
            px[x, y] = mix(c1, c2, t)
    draw = ImageDraw.Draw(image, "RGBA")
    # soft paper-like bubbles, deliberately low contrast behind the focal art
    for _ in range(14):
        x, y = rng.randrange(-40, 512), rng.randrange(-40, 288)
        r = rng.randrange(12, 45)
        draw.ellipse((x - r, y - r, x + r, y + r), fill=light + (18,))
    draw.rounded_rectangle((14, 14, 498, 274), radius=32, outline=light + (95,), width=3)
    draw.ellipse((30, 202, 126, 298), fill=(12, 37, 49, 42))
    draw.ellipse((386, -30, 535, 116), fill=(255, 255, 255, 25))
    dark = mix(c1, (9, 22, 35), .48)
    if slug in TILE_GAMES:
        draw_tiles(draw, rng, accent, light, dark)
    elif slug in BOARD_GAMES:
        draw_board(draw, rng, accent, light, dark, slug)
    elif slug in ARCADE_GAMES:
        draw_arcade(draw, rng, accent, light, dark, slug)
    elif slug in {"word-search", "wordle"}:
        draw_words(draw, rng, accent, light, dark, slug)
    elif slug == "bolt-sort":
        for i, x in enumerate((149, 206, 263, 320)):
            fill = [accent, light, (111, 212, 185), (244, 124, 107)][i]
            draw.rounded_rectangle((x - 13, 69, x + 13, 224), radius=10, fill=dark, outline=light, width=3)
            for j in range(3):
                draw.ellipse((x - 17, 83 + j * 40, x + 17, 117 + j * 40), fill=fill)
            draw.ellipse((x - 13, 47, x + 13, 73), fill=fill, outline=light, width=3)
    elif slug == "color-connect":
        draw.rounded_rectangle((96, 42, 416, 246), radius=24, fill=dark, outline=light, width=3)
        pts = [(142, 82, accent), (370, 82, accent), (142, 201, light), (370, 201, light), (225, 82, (111, 212, 185)), (285, 201, (111, 212, 185))]
        draw.line((142, 82, 210, 140, 142, 201), fill=accent, width=12, joint="curve")
        draw.line((370, 82, 302, 140, 370, 201), fill=light, width=12, joint="curve")
        draw.line((225, 82, 285, 140, 285, 201), fill=(111, 212, 185), width=12, joint="curve")
        for x, y, fill in pts:
            draw.ellipse((x - 16, y - 16, x + 16, y + 16), fill=dark, outline=fill, width=8)
            draw.ellipse((x - 8, y - 8, x + 8, y + 8), fill=fill)
    else:
        draw_board(draw, rng, accent, light, dark, slug)
    # Friendly signature mark, kept small so it never competes with the game motif.
    paw(draw, 58, 230, .7, light + (170,))
    buddy(draw, 454, 231, .43, light, accent, eye=dark)
    return image


try:
    FONT = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 48)
except OSError:
    FONT = ImageFont.load_default()


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    for slug in PALETTES:
        path = OUT / f"mw_game_{slug.replace('-', '_')}.png"
        render(slug).save(path, format="PNG", optimize=True)
        print(path)
