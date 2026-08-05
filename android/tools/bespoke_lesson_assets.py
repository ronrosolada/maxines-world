#!/usr/bin/env python3
"""Generate topic-specific, scene-based lesson illustrations.

This module intentionally avoids the old four-card board template. Each lesson
gets a small illustrated scene whose objects are selected from its topic profile
and varied deterministically by lesson ID.
"""
from __future__ import annotations

import hashlib
import html
import math
import random
from typing import Callable

from content_review import canonical_subject, profile_for


PALETTES = {
    "english": ("#FFF4D6", "#8DD3C7", "#2E6F6D", "#F2A65A"),
    "mathematics": ("#EAF6FF", "#80B1D3", "#315A7D", "#F2C14E"),
    "science": ("#F4FFE4", "#B3DE69", "#39734A", "#F28E8E"),
    "filipino": ("#FFF0F5", "#F6A6C1", "#7A3152", "#8EC6FF"),
    "araling-panlipunan": ("#FFF1E6", "#FFB26B", "#7A4528", "#6CB4A8"),
    "gmrc": ("#F1EEFF", "#B8A4E8", "#503B78", "#F4C95D"),
    "makabansa": ("#EFFFF8", "#72C9A3", "#24624A", "#F08A5D"),
}


MOTIFS: dict[str, list[str]] = {
    "picture": ["magnifier", "person", "tree", "house", "kite", "dog"],
    "characters": ["person", "cat", "speech", "book", "house", "friendship"],
    "ending": ["path", "flag", "home", "star", "book", "seedling"],
    "diary": ["diary", "pencil", "person", "heart", "calendar", "plant"],
    "telling": ["sentence", "period", "pencil", "book", "speech", "star"],
    "capitalization": ["capital_a", "pencil", "sentence", "book", "period", "star"],
    "nouns": ["person", "house", "tree", "book", "name_tag", "school"],
    "plural_s": ["one_many", "people", "tree", "book", "counting", "basket"],
    "plural_es": ["one_many", "boxes", "brush", "book", "counting", "basket"],
    "vowels": ["vowel_cards", "mouth", "book", "sound", "pencil", "star"],
    "syllable": ["clap", "sound", "word_blocks", "person", "book", "rhythm"],
    "be_verb": ["people", "speech", "link", "book", "sentence", "star"],
    "tense": ["yesterday", "today", "tomorrow", "clock", "person", "calendar"],
    "blend": ["letter_blocks", "bridge", "sound", "book", "pencil", "star"],
    "digraph": ["letter_pair", "sound", "book", "speech", "magnifier", "star"],
    "intonation": ["sound", "speech", "arrow", "person", "ear", "star"],
    "sight": ["word_cards", "path", "book", "person", "star", "pencil"],
    "possessive": ["name_tag", "backpack", "person", "house", "heart", "book"],
    "cause_effect": ["cloud", "rain", "seedling", "arrow", "tree", "sun"],
    "main_detail": ["magnifier", "book", "person", "detail_dots", "star", "tree"],
    "graph": ["picture_graph", "objects", "person", "bar_graph", "magnifier", "book"],
    "root": ["tree_root", "word_cards", "book", "pencil", "leaf", "star"],
    "synonym": ["word_twins", "bridge", "book", "speech", "star", "person"],
    "compound": ["two_sentences", "bridge", "book", "person", "link", "star"],
    "sentence_parts": ["sentence_parts", "person", "book", "pencil", "arrow", "speech"],
    "sentence_sequence": ["sequence", "numbered_cards", "path", "book", "arrow", "star"],
    "informational": ["fact_cards", "magnifier", "book", "graph", "person", "star"],
    "story_comprehension": ["story_scene", "person", "cat", "book", "speech", "home"],
    "retell": ["sequence", "story_scene", "person", "book", "arrow", "home"],
    "vocabulary": ["word_cards", "magnifier", "book", "speech", "person", "star"],
    "experience": ["diary", "person", "heart", "calendar", "pencil", "home"],
    "sentence": ["sentence", "pencil", "book", "period", "speech", "star"],
    "general_english": ["book", "person", "pencil", "speech", "star", "house"],
    "polite": ["speech", "people", "hand_wave", "heart", "school", "book"],
    "number": ["number_tiles", "place_value", "counting", "number_line", "person", "star"],
    "place_value": ["place_value", "number_tiles", "base_ten", "counting", "arrow", "person"],
    "number_words": ["number_tiles", "word_cards", "pencil", "book", "counting", "star"],
    "money": ["coin", "money_notes", "number_tiles", "market", "person", "basket"],
    "compare": ["balance", "greater_less", "number_tiles", "arrow", "person", "star"],
    "number_line": ["number_line", "marker", "arrow", "number_tiles", "person", "flag"],
    "ordinal": ["race", "medals", "number_line", "people", "flag", "star"],
    "round": ["rounding_arc", "number_line", "number_tiles", "arrow", "person", "star"],
    "addition": ["addition_counters", "number_tiles", "plus", "person", "basket", "star"],
    "subtraction": ["takeaway_counters", "number_tiles", "minus", "person", "basket", "arrow"],
    "difference": ["balance", "number_tiles", "minus", "arrow", "person", "star"],
    "multi_add": ["addition_counters", "baskets", "number_tiles", "plus", "person", "star"],
    "multiplication": ["array", "groups", "number_tiles", "times", "person", "star"],
    "area": ["grid", "rectangle", "square_units", "ruler", "person", "star"],
    "geometry": ["shapes", "ruler", "line_segment", "angle", "person", "star"],
    "capacity_measure": ["containers", "cup", "ruler", "water", "person", "arrow"],
    "capacity_estimate": ["containers", "water", "guess", "ruler", "person", "star"],
    "capacity_compare": ["containers", "balance", "water", "arrow", "person", "star"],
    "bar_graph": ["bar_graph", "data_dots", "person", "picture_graph", "magnifier", "star"],
    "probability": ["spinner", "coin", "chance_cards", "person", "question", "star"],
    "pattern": ["pattern_blocks", "sequence", "arrow", "person", "star", "number_tiles"],
    "division": ["sharing_groups", "counters", "number_tiles", "arrow", "person", "basket"],
    "fraction": ["fraction_circle", "sharing", "number_line", "person", "pie", "star"],
    "transformation": ["shape_before_after", "arrow", "grid", "person", "star", "ruler"],
    "general_math": ["number_tiles", "ruler", "shapes", "person", "book", "star"],
    "materials": ["rock", "metal", "wood", "water", "magnifier", "recycle"],
    "living": ["plant", "animal", "person", "sun", "water", "habitat"],
    "motion": ["ramp", "ball", "arrow", "push", "person", "surface"],
    "light_sound": ["sun", "lamp", "sound", "ear", "vibration", "person"],
    "sky_weather": ["sun", "cloud", "rain", "moon", "wind", "weather_cards"],
    "general_science": ["lab", "magnifier", "plant", "rock", "sun", "person"],
    "energy": ["lamp", "battery", "lightning", "heat", "person", "check"],
    "greetings": ["speech", "people", "heart", "school", "book", "hand_wave"],
    "root_filipino": ["tree_root", "word_cards", "book", "pencil", "leaf", "star"],
    "writing": ["pencil", "notebook", "letters", "person", "book", "check"],
    "context": ["dictionary", "magnifier", "word_cards", "book", "person", "speech"],
    "summary": ["story_scene", "three_cards", "arrow", "book", "person", "star"],
    "paragraph": ["notebook", "sentence_lines", "person", "pencil", "house", "tree"],
    "word_use": ["word_cards", "speech", "person", "book", "house", "star"],
    "general_filipino": ["book", "pencil", "person", "speech", "house", "star"],
    "story_comprehension": ["story_scene", "question", "book", "person", "speech", "magnifier"],
    "book_parts": ["book", "magnifier", "word_cards", "person", "page_tabs", "star"],
    "instructions": ["numbered_cards", "arrow", "person", "checklist", "path", "star"],
    "pronouns": ["person", "word_cards", "arrow", "speech", "book", "star"],
    "story_elements": ["story_scene", "person", "home", "problem", "arrow", "book"],
    "ending_filipino": ["story_scene", "path", "home", "flag", "star", "book"],
    "inference": ["magnifier", "clue_cards", "question", "book", "arrow", "star"],
    "animal_care": ["animal", "food_bowl", "water", "home", "person", "heart"],
    "faith": ["sun", "heart", "person", "hands", "house", "star"],
    "respect": ["ear", "people", "speech", "heart", "school", "hand_wave"],
    "care": ["hands", "heart", "plant", "person", "water", "home"],
    "responsibility": ["checklist", "backpack", "person", "clock", "home", "star"],
    "discipline": ["checklist", "clock", "path", "person", "school", "arrow"],
    "cooperation": ["hands", "people", "bridge", "house", "heart", "star"],
    "honesty": ["open_box", "heart", "person", "speech", "check", "sun"],
    "initiative": ["lightbulb", "person", "arrow", "seedling", "star", "path"],
    "patience": ["seedling", "clock", "person", "sun", "path", "heart"],
    "citizenship": ["flag", "people", "house", "tree", "heart", "check"],
    "judgment": ["magnifier", "two_paths", "checklist", "person", "question", "star"],
    "gratitude": ["heart", "hands", "people", "sun", "home", "star"],
    "self_confidence": ["mirror", "star", "person", "path", "heart", "sun"],
    "general_gmrc": ["people", "heart", "home", "speech", "star", "check"],
    "map_symbols": ["map", "compass", "flag", "legend", "route", "magnifier"],
    "population": ["people", "bar_graph", "map", "counting", "magnifier", "house"],
    "terrain": ["mountain", "river", "valley", "map", "sun", "tree"],
    "drainage": ["river", "mountain", "water_arrows", "map", "cloud", "tree"],
    "land_water": ["mountain", "river", "lake", "tree", "house", "map"],
    "hazard": ["hazard", "map", "route", "checklist", "people", "house"],
    "resources": ["leaf", "tree", "river", "recycle", "people", "basket"],
    "environment_map": ["map", "leaf", "hazard", "people", "river", "bar_graph"],
    "province_origins": ["timeline", "map", "house", "book", "people", "flag"],
    "change_continuity": ["timeline", "old_new_house", "tree", "arrow", "people", "book"],
    "livelihood": ["market", "people", "basket", "farm", "house", "map"],
    "provincial_symbols": ["flag", "seal", "map", "art", "star", "house"],
    "arts_place": ["art", "music", "people", "house", "flag", "star"],
    "community_helper": ["helper", "hands", "house", "people", "heart", "flag"],
    "regional_identity": ["map", "flag", "people", "house", "heart", "star"],
    "cultural_practice": ["people", "art", "house", "tree", "music", "basket"],
    "historical_site": ["map", "historic_house", "timeline", "book", "flag", "magnifier"],
    "cultural_map": ["map", "art", "house", "flag", "people", "route"],
    "general_ap": ["map", "people", "house", "book", "flag", "tree"],
    "music": ["music", "sound", "people", "rhythm", "flag", "art"],
    "identity": ["people", "flag", "house", "heart", "art", "map"],
    "active_citizen": ["people", "flag", "hands", "house", "tree", "check"],
    "culture": ["art", "people", "house", "tree", "music", "basket"],
    "community_history": ["old_new_house", "timeline", "people", "tree", "book", "flag"],
}


def esc(value: object) -> str:
    return html.escape(str(value), quote=True)


def tag(name: str, attrs: dict[str, object], body: str = "") -> str:
    rendered = " ".join(f'{k}="{esc(v)}"' for k, v in attrs.items())
    return f"<{name} {rendered}>{body}</{name}>"


def circle(x: float, y: float, r: float, fill: str, stroke: str = "none", width: float = 0) -> str:
    return tag("circle", {"cx": round(x, 1), "cy": round(y, 1), "r": round(r, 1), "fill": fill, "stroke": stroke, "stroke-width": width})


def ellipse(x: float, y: float, rx: float, ry: float, fill: str, stroke: str = "none", width: float = 0) -> str:
    return tag("ellipse", {"cx": round(x, 1), "cy": round(y, 1), "rx": round(rx, 1), "ry": round(ry, 1), "fill": fill, "stroke": stroke, "stroke-width": width})


def rect(x: float, y: float, w: float, h: float, fill: str, stroke: str = "none", width: float = 0, rx: float = 0) -> str:
    return tag("rect", {"x": round(x, 1), "y": round(y, 1), "width": round(w, 1), "height": round(h, 1), "rx": rx, "fill": fill, "stroke": stroke, "stroke-width": width})


def line(x1: float, y1: float, x2: float, y2: float, stroke: str, width: float = 4, dash: str | None = None) -> str:
    attrs: dict[str, object] = {"x1": round(x1, 1), "y1": round(y1, 1), "x2": round(x2, 1), "y2": round(y2, 1), "stroke": stroke, "stroke-width": width, "stroke-linecap": "round"}
    if dash:
        attrs["stroke-dasharray"] = dash
    return tag("line", attrs)


def path(d: str, fill: str, stroke: str = "none", width: float = 0) -> str:
    return tag("path", {"d": d, "fill": fill, "stroke": stroke, "stroke-width": width, "stroke-linecap": "round", "stroke-linejoin": "round"})


def polygon(points: str, fill: str, stroke: str = "none", width: float = 0) -> str:
    return tag("polygon", {"points": points, "fill": fill, "stroke": stroke, "stroke-width": width, "stroke-linejoin": "round"})


def text(x: float, y: float, value: str, size: int, fill: str, weight: str = "700", anchor: str = "middle") -> str:
    return tag("text", {"x": round(x, 1), "y": round(y, 1), "font-family": "sans-serif", "font-size": size, "font-weight": weight, "text-anchor": anchor, "fill": fill}, esc(value))


def stable_rng(lesson_id: str) -> random.Random:
    seed = int(hashlib.sha256(lesson_id.encode()).hexdigest()[:16], 16)
    return random.Random(seed)


def person(x: float, y: float, scale: float, shirt: str, skin: str, ink: str, pose: int = 0) -> str:
    s = scale
    arm = 24 * s if pose % 2 == 0 else -24 * s
    return (
        circle(x, y - 42 * s, 15 * s, skin, ink, 3 * s)
        + path(f"M{x-24*s},{y+32*s} Q{x-22*s},{y-3*s} {x},{y-12*s} Q{x+22*s},{y-3*s} {x+24*s},{y+32*s} Z", shirt, ink, 3 * s)
        + line(x - 16 * s, y + 2 * s, x + arm, y - 18 * s, skin, 6 * s)
        + line(x + 16 * s, y + 2 * s, x - arm, y - 18 * s, skin, 6 * s)
        + line(x - 9 * s, y + 31 * s, x - 16 * s, y + 58 * s, ink, 5 * s)
        + line(x + 9 * s, y + 31 * s, x + 16 * s, y + 58 * s, ink, 5 * s)
    )


def tree(x: float, y: float, scale: float, leaf: str, ink: str) -> str:
    s = scale
    return rect(x - 9 * s, y, 18 * s, 70 * s, "#9A6A45", ink, 3 * s, 5 * s) + circle(x, y - 18 * s, 36 * s, leaf, ink, 4 * s) + circle(x - 28 * s, y + 2 * s, 25 * s, leaf, ink, 4 * s) + circle(x + 28 * s, y + 2 * s, 25 * s, leaf, ink, 4 * s)


def house(x: float, y: float, scale: float, roof: str, ink: str, old: bool = False) -> str:
    s = scale
    wall = "#FFFDF5" if not old else "#E9D3B2"
    return polygon(f"{x-58*s},{y} {x},{y-52*s} {x+58*s},{y}", roof, ink, 4 * s) + rect(x - 47 * s, y, 94 * s, 65 * s, wall, ink, 4 * s, 5 * s) + rect(x - 10 * s, y + 28 * s, 20 * s, 37 * s, "#9A6A45", ink, 3 * s, 3 * s) + rect(x - 34 * s, y + 14 * s, 18 * s, 18 * s, "#8DD3E8", ink, 3 * s, 2 * s) + rect(x + 16 * s, y + 14 * s, 18 * s, 18 * s, "#8DD3E8", ink, 3 * s, 2 * s)


def book(x: float, y: float, scale: float, cover: str, ink: str) -> str:
    s = scale
    return path(f"M{x},{y-42*s} Q{x-34*s},{y-54*s} {x-62*s},{y-38*s} L{x-62*s},{y+38*s} Q{x-30*s},{y+24*s} {x},{y+40*s} Z", "#FFFDF5", ink, 4 * s) + path(f"M{x},{y-42*s} Q{x+34*s},{y-54*s} {x+62*s},{y-38*s} L{x+62*s},{y+38*s} Q{x+30*s},{y+24*s} {x},{y+40*s} Z", "#FFFDF5", ink, 4 * s) + line(x, y - 42 * s, x, y + 40 * s, cover, 4 * s) + line(x - 42 * s, y - 12 * s, x - 14 * s, y - 18 * s, cover, 3 * s) + line(x + 14 * s, y - 18 * s, x + 42 * s, y - 12 * s, cover, 3 * s)


def speech(x: float, y: float, scale: float, fill: str, ink: str, mark: str = "…") -> str:
    s = scale
    return path(f"M{x-55*s},{y-34*s} Q{x-55*s},{y-52*s} {x-37*s},{y-52*s} L{x+42*s},{y-52*s} Q{x+60*s},{y-52*s} {x+60*s},{y-34*s} L{x+60*s},{y+10*s} Q{x+60*s},{y+28*s} {x+42*s},{y+28*s} L{x+8*s},{y+28*s} L{x-8*s},{y+50*s} L{x-4*s},{y+28*s} L{x-37*s},{y+28*s} Q{x-55*s},{y+28*s} {x-55*s},{y+10*s} Z", fill, ink, 4 * s) + text(x + 2 * s, y + 8 * s, mark, int(30 * s), ink)


def star(x: float, y: float, scale: float, fill: str, ink: str) -> str:
    pts=[]
    for i in range(10):
        a=-math.pi/2+i*math.pi/5; r=(42 if i%2==0 else 19)*scale
        pts.append(f"{x+r*math.cos(a):.1f},{y+r*math.sin(a):.1f}")
    return polygon(" ".join(pts), fill, ink, 4*scale)


def flag(x: float, y: float, scale: float, fill: str, ink: str) -> str:
    s=scale
    return line(x, y-62*s, x, y+54*s, ink, 5*s) + path(f"M{x+2*s},{y-55*s} Q{x+38*s},{y-65*s} {x+65*s},{y-48*s} L{x+65*s},{y-10*s} Q{x+34*s},{y-27*s} {x+2*s},{y-16*s} Z", fill, ink, 4*s) + star(x+21*s,y-35*s,.35*s,"#FFD66B",ink)


def sun(x: float, y: float, scale: float, fill: str, ink: str) -> str:
    s=scale; out=""
    for dx,dy in ((0,-55),(39,-39),(55,0),(39,39),(0,55),(-39,39),(-55,0),(-39,-39)):
        out += line(x+dx*s,y+dy*s,x+dx*1.35*s,y+dy*1.35*s,ink,4*s)
    return out+circle(x,y,30*s,fill,ink,4*s)


def cloud(x: float, y: float, scale: float, fill: str, ink: str, rain: bool=False) -> str:
    s=scale; out=circle(x-24*s,y+4*s,22*s,fill,ink,3*s)+circle(x+4*s,y-12*s,31*s,fill,ink,3*s)+circle(x+33*s,y+4*s,21*s,fill,ink,3*s)+rect(x-46*s,y+3*s,92*s,30*s,fill,ink,3*s,10*s)
    if rain:
        for i in (-24,0,24): out+=line(x+i*s,y+48*s,x+i*s-8*s,y+68*s,"#65A9D6",4*s)
    return out


def map_icon(x: float,y: float,s: float,accent: str,ink: str)->str:
    return path(f"M{x-68*s},{y-42*s} L{x-20*s},{y-57*s} L{x+26*s},{y-40*s} L{x+68*s},{y-55*s} L{x+68*s},{y+40*s} L{x+24*s},{y+55*s} L{x-20*s},{y+38*s} L{x-68*s},{y+52*s} Z","#FFFDF5",ink,4*s)+path(f"M{x-48*s},{y+20*s} Q{x-22*s},{y-25*s} {x+5*s},{y+5*s} T{x+48*s},{y-25*s}","none",accent,7*s)+circle(x+32*s,y-18*s,8*s,accent,ink,3*s)


def mountain(x: float,y: float,s: float,accent: str,ink: str)->str:
    return polygon(f"{x-78*s},{y+48*s} {x-22*s},{y-52*s} {x+4*s},{y-13*s} {x+30*s},{y-38*s} {x+82*s},{y+48*s}",accent,ink,4*s)+polygon(f"{x-22*s},{y-52*s} {x-2*s},{y-17*s} {x-12*s},{y-11*s}","#FFFDF5",ink,3*s)+path(f"M{x-60*s},{y+48*s} Q{x},{y+4*s} {x+60*s},{y+48*s}","#A7D9D0",ink,4*s)


def river(x: float,y: float,s: float,accent: str,ink: str)->str:
    return path(f"M{x-70*s},{y-56*s} Q{x-30*s},{y-10*s} {x-47*s},{y+50*s} Q{x-12*s},{y+67*s} {x+11*s},{y+18*s} Q{x+33*s},{y-15*s} {x+72*s},{y-55*s}",accent,ink,8*s)+line(x-38*s,y-24*s,x-20*s,y-12*s,"#FFFDF5",3*s)+line(x+11*s,y+21*s,x+31*s,y+31*s,"#FFFDF5",3*s)


def number_tile(x: float,y: float,s: float,number: str,fill: str,ink: str)->str:
    return rect(x-32*s,y-34*s,64*s,68*s,fill,ink,5*s,9*s)+text(x,y+12*s,number,int(34*s),ink)


def array_icon(x: float,y: float,s: float,fill: str,ink: str,rows: int=3,cols: int=4)->str:
    out=""
    for r in range(rows):
        for c in range(cols): out+=circle(x+(c-(cols-1)/2)*27*s,y+(r-(rows-1)/2)*27*s,8*s,fill,ink,3*s)
    return out


def bar_graph(x: float,y: float,s: float,accent: str,ink: str)->str:
    out=line(x-62*s,y+55*s,x+65*s,y+55*s,ink,5*s)+line(x-62*s,y-55*s,x-62*s,y+55*s,ink,5*s)
    for i,h in enumerate((28,52,38,72)):
        out+=rect(x+(-43+i*31)*s,y+55*s-h*s,18*s,h*s,accent if i%2==0 else "#FFD66B",ink,3*s,4*s)
    return out


def shapes(x: float,y: float,s: float,accent: str,ink: str)->str:
    return circle(x-30*s,y-15*s,22*s,accent,ink,4*s)+rect(x+5*s,y-38*s,45*s,45*s,"#FFFDF5",ink,4*s,5*s)+polygon(f"{x-48*s},{y+48*s} {x-20*s},{y+1*s} {x+8*s},{y+48*s}",accent,ink,4*s)


def grid(x: float,y: float,s: float,accent: str,ink: str)->str:
    return rect(x-62*s,y-52*s,124*s,104*s,"#FFFDF5",ink,4*s,6*s)+"".join(line(x-31*s+i*31*s,y-50*s,x-31*s+i*31*s,y+50*s,accent,3*s) for i in range(3))+"".join(line(x-60*s,y-26*s+i*26*s,x+60*s,y-26*s+i*26*s,accent,3*s) for i in range(3))


def fraction_icon(x: float,y: float,s: float,accent: str,ink: str)->str:
    r=48*s
    return circle(x,y,r,"#FFFDF5",ink,4*s)+path(f"M{x},{y} L{x},{y-r} A{r},{r} 0 0 1 {x+r},{y} Z",accent,ink,3*s)+line(x-r,y,x+r,y,ink,3*s)


def clock(x: float,y: float,s: float,accent: str,ink: str)->str:
    return circle(x,y,48*s,"#FFFDF5",ink,4*s)+line(x,y,x,y-27*s,accent,5*s)+line(x,y,x+25*s,y+14*s,ink,5*s)+circle(x,y,6*s,ink)


def speech_lines(x: float,y: float,s: float,fill: str,ink: str)->str:
    return rect(x-65*s,y-46*s,130*s,92*s,fill,ink,4*s,12*s)+line(x-43*s,y-19*s,x+38*s,y-19*s,ink,4*s)+line(x-43*s,y+2*s,x+22*s,y+2*s,ink,4*s)+line(x-43*s,y+23*s,x+6*s,y+23*s,ink,4*s)


def draw_motif(name: str,x: float,y: float,s: float,accent: str,secondary: str,ink: str,rng: random.Random)->str:
    if name in {"people", "characters"}: return person(x-34*s,y,s*.8,accent,"#F2B58F",ink,0)+person(x+34*s,y,s*.8,secondary,"#D98D68",ink,1)
    if name in {"letters", "vowel_cards", "letter_cards", "word_cards", "numbered_cards", "clue_cards", "fact_cards", "chance_cards"}:
        labels = ["1", "2", "3"] if name == "numbered_cards" else ["A", "B", "C"]
        return "".join(rect(x-70*s+i*50*s,y-34*s,42*s,68*s,accent if i==0 else (secondary if i==1 else "#FFFDF5"),ink,3*s,6*s)+text(x-49*s+i*50*s,y+10*s,labels[i],int(25*s),ink) for i in range(3))
    if name == "page_tabs": return book(x,y,s,accent,ink)+"".join(rect(x-65*s+i*28*s,y-57*s,20*s,12*s,secondary if i%2 else accent,ink,2*s,2*s) for i in range(4))
    if name == "story_scene": return house(x+35*s,y+22*s,s*.62,accent,ink)+person(x-35*s,y+20*s,s*.62,secondary,"#F2B58F",ink,0)+tree(x-78*s,y+10*s,s*.42,accent,ink)
    if name == "problem": return cloud(x,y-5*s,s*.65,"#FFFDF5",ink,False)+text(x,y+12*s,"?",int(34*s),accent)
    if name == "food_bowl": return ellipse(x,y+20*s,45*s,18*s,secondary,ink,4*s)+path(f"M{x-42*s},{y+20*s} Q{x},{y+75*s} {x+42*s},{y+20*s}","#FFFDF5",ink,4*s)+circle(x-18*s,y+6*s,5*s,accent,ink,2*s)+circle(x+5*s,y+2*s,5*s,accent,ink,2*s)+circle(x+25*s,y+9*s,5*s,accent,ink,2*s)
    if name == "money" or name == "coin": return circle(x-28*s,y,31*s,"#FFD66B",ink,4*s)+circle(x+30*s,y+8*s,25*s,"#FFE59A",ink,4*s)+text(x-28*s,y+11*s,"₱",int(29*s),ink)+text(x+30*s,y+17*s,"¢",int(24*s),ink)
    if name == "money_notes": return rect(x-66*s,y-27*s,82*s,52*s,"#8DD3A7",ink,4*s,5*s)+rect(x-16*s,y-42*s,82*s,52*s,"#9EC5E8",ink,4*s,5*s)+text(x+25*s,y-7*s,"₱",int(25*s),ink)
    if name == "battery": return rect(x-42*s,y-27*s,84*s,54*s,"#FFFDF5",ink,4*s,5*s)+rect(x+42*s,y-10*s,10*s,20*s,ink)+rect(x-30*s,y-15*s,38*s,30*s,accent,ink,3*s,3*s)
    if name == "lightning": return polygon(f"{x+10*s},{y-58*s} {x-35*s},{y+4*s} {x-5*s},{y+4*s} {x-20*s},{y+57*s} {x+42*s},{y-15*s} {x+12*s},{y-15*s}","#FFD66B",ink,4*s)
    if name == "heat": return "".join(path(f"M{x-35*s+i*22*s},{y+35*s} Q{x-48*s+i*22*s},{y-5*s} {x-35*s+i*22*s},{y-42*s}","none",accent,5*s) for i in range(4))
    if name == "race": return line(x-70*s,y+34*s,x+70*s,y+34*s,ink,5*s)+person(x-42*s,y,s*.5,accent,"#F2B58F",ink,0)+person(x+8*s,y-10*s,s*.5,secondary,"#D98D68",ink,1)+flag(x+55*s,y-38*s,s*.4,accent,ink)
    if name == "medals": return circle(x-35*s,y,27*s,accent,ink,4*s)+circle(x+35*s,y,27*s,secondary,ink,4*s)+text(x-35*s,y+10*s,"1",int(25*s),ink)+text(x+35*s,y+10*s,"2",int(25*s),ink)
    if name == "spinner": return circle(x,y,48*s,"#FFFDF5",ink,4*s)+line(x,y,x+34*s,y-20*s,accent,6*s)+circle(x,y,8*s,secondary,ink,3*s)+"".join(line(x+math.cos(i*math.pi/2)*37*s,y+math.sin(i*math.pi/2)*37*s,x+math.cos(i*math.pi/2)*48*s,y+math.sin(i*math.pi/2)*48*s,ink,3*s) for i in range(4))
    if name == "question": return circle(x,y,43*s,"#FFFDF5",ink,4*s)+text(x,y+18*s,"?",int(55*s),accent)
    if name == "sharing" or name == "sharing_groups": return array_icon(x-35*s,y,s*.6,accent,ink,2,3)+arrow(x+20*s,y,secondary,ink,s*.45)+array_icon(x+63*s,y,s*.55,secondary,ink,2,3)
    if name == "three_cards": return "".join(rect(x-70*s+i*52*s,y-34*s,42*s,68*s,accent if i==0 else (secondary if i==1 else "#FFFDF5"),ink,3*s,6*s)+text(x-49*s+i*52*s,y+10*s,str(i+1),int(24*s),ink) for i in range(3))
    if name == "base_ten": return rect(x-58*s,y-24*s,35*s,48*s,accent,ink,3*s,4*s)+"".join(rect(x-10*s+i*13*s,y-25*s,9*s,50*s,secondary,ink,2*s,2*s) for i in range(4))+array_icon(x+47*s,y,s*.45,"#FFFDF5",ink,2,2)
    if name == "greater_less": return text(x,y+18*s,"< > =",int(35*s),accent)
    if name == "shape_before_after": return shapes(x-42*s,y,s*.65,accent,ink)+arrow(x+12*s,y,secondary,ink,s*.45)+shapes(x+61*s,y,s*.65,secondary,ink)
    if name == "line_segment": return line(x-55*s,y+20*s,x+55*s,y-20*s,accent,7*s)+circle(x-55*s,y+20*s,9*s,secondary,ink,3*s)+circle(x+55*s,y-20*s,9*s,secondary,ink,3*s)
    if name == "angle": return line(x-45*s,y+35*s,x,y-35*s,ink,6*s)+line(x,y-35*s,x+50*s,y+35*s,ink,6*s)+path(f"M{x-20*s},{y+4*s} Q{x},{y-15*s} {x+21*s},{y+4*s}","none",accent,4*s)
    if name == "grid": return grid(x,y,s,accent,ink)
    if name=="person": return person(x,y,s,accent,"#F2B58F",ink,rng.randrange(4))
    if name=="people": return person(x-34*s,y,s*.8,accent,"#F2B58F",ink,0)+person(x+34*s,y,s*.8,secondary,"#D98D68",ink,1)
    if name=="cat": return circle(x,y-25*s,28*s,"#F2A65A",ink,4*s)+polygon(f"{x-25*s},{y-43*s} {x-16*s},{y-75*s} {x-2*s},{y-50*s} {x+16*s},{y-75*s} {x+26*s},{y-43*s}","#F2A65A",ink,4*s)+circle(x-10*s,y-27*s,4*s,ink)+circle(x+10*s,y-27*s,4*s,ink)+path(f"M{x-10*s},{y-6*s} Q{x},{y+5*s} {x+10*s},{y-6*s}","none",ink,4*s)
    if name in ("tree","plant","seedling","tree_root"):
        out=tree(x,y,s,accent,ink)
        if name=="plant": out+=line(x,y+68*s,x,y+95*s,ink,4*s)+path(f"M{x},{y+86*s} Q{x-35*s},{y+60*s} {x-28*s},{y+42*s} Q{x-4*s},{y+45*s} {x},{y+72*s} Z",secondary,ink,3*s)
        if name=="tree_root": out+=path(f"M{x},{y+67*s} Q{x-27*s},{y+84*s} {x-44*s},{y+106*s} M{x},{y+67*s} Q{x+26*s},{y+86*s} {x+44*s},{y+106*s}","none",ink,4*s)
        return out
    if name in ("house","home"): return house(x,y,s,accent,ink)
    if name=="historic_house": return house(x,y,s,"#B77B54",ink,True)+rect(x-4*s,y+17*s,8*s,9*s,"#FFD66B",ink,2*s)
    if name=="old_new_house": return house(x-42*s,y,s*.72,"#B77B54",ink,True)+arrow(x+22*s,y+10*s,secondary,ink,s*.75)+house(x+78*s,y,s*.72,accent,ink)
    if name=="book" or name=="diary": return book(x,y,s,accent,ink)
    if name=="notebook": return rect(x-52*s,y-60*s,104*s,120*s,"#FFFDF5",ink,4*s,8*s)+line(x-32*s,y-27*s,x+32*s,y-27*s,accent,4*s)+line(x-32*s,y,x+32*s,y,accent,4*s)+line(x-32*s,y+27*s,x+18*s,y+27*s,accent,4*s)+line(x-40*s,y-60*s,x-40*s,y+60*s,secondary,5*s)
    if name in ("speech","speech_lines"): return speech(x,y,s,"#FFFDF5",ink,"Hi!") if name=="speech" else speech_lines(x,y,s,"#FFFDF5",ink)
    if name=="star": return star(x,y,s,accent,ink)
    if name=="heart": return path(f"M{x},{y+45*s} C{x-75*s},{y-2*s} {x-44*s},{y-54*s} {x},{y-18*s} C{x+44*s},{y-54*s} {x+75*s},{y-2*s} {x},{y+45*s} Z",accent,ink,4*s)
    if name=="flag": return flag(x,y,s,accent,ink)
    if name=="sun": return sun(x,y,s,accent,ink)
    if name=="cloud": return cloud(x,y,s,"#FFFDF5",ink,False)
    if name=="rain": return cloud(x,y,s,"#FFFDF5",ink,True)
    if name=="mountain": return mountain(x,y,s,accent,ink)
    if name in ("river","water_arrows"): return river(x,y,s,accent,ink)+((arrow(x+40*s,y+35*s,"#FFFDF5",ink,s*.45)) if name=="water_arrows" else "")
    if name=="map": return map_icon(x,y,s,accent,ink)
    if name=="compass": return circle(x,y,48*s,"#FFFDF5",ink,4*s)+polygon(f"{x},{y-38*s} {x+11*s},{y+8*s} {x},{y+38*s} {x-11*s},{y+8*s}",accent,ink,3*s)+polygon(f"{x-38*s},{y} {x+8*s},{y-11*s} {x+38*s},{y} {x+8*s},{y+11*s}","#FFFDF5",ink,3*s)
    if name=="legend": return rect(x-60*s,y-48*s,120*s,96*s,"#FFFDF5",ink,4*s,8*s)+circle(x-30*s,y-20*s,10*s,accent,ink,3*s)+line(x-10*s,y-20*s,x+40*s,y-20*s,ink,4*s)+rect(x-40*s,y+8*s,20*s,20*s,secondary,ink,3*s)+line(x-10*s,y+18*s,x+40*s,y+18*s,ink,4*s)
    if name=="route": return line(x-55*s,y+35*s,x-20*s,y-15*s,accent,7*s)+line(x-20*s,y-15*s,x+22*s,y+20*s,accent,7*s)+line(x+22*s,y+20*s,x+55*s,y-35*s,accent,7*s)+circle(x-55*s,y+35*s,9*s,"#FFFDF5",ink,3*s)+circle(x+55*s,y-35*s,11*s,secondary,ink,3*s)
    if name=="hazard": return polygon(f"{x},{y-55*s} {x+54*s},{y+45*s} {x-54*s},{y+45*s}","#FFD66B",ink,5*s)+line(x,y-28*s,x,y+14*s,ink,8*s)+circle(x,y+30*s,5*s,ink)
    if name=="bar_graph" or name=="picture_graph": return bar_graph(x,y,s,accent,ink)
    if name=="array" or name=="counting": return array_icon(x,y,s,accent,ink,3,4)
    if name=="groups": return array_icon(x-35*s,y,s*.65,accent,ink,2,3)+array_icon(x+40*s,y,s*.65,secondary,ink,2,3)
    if name=="number_tiles": return number_tile(x-42*s,y,s*.65,"3",accent,ink)+number_tile(x+42*s,y,s*.65,"7",secondary,ink)
    if name=="place_value": return number_tile(x-58*s,y,s*.55,"1",accent,ink)+number_tile(x,y,s*.55,"0",secondary,ink)+number_tile(x+58*s,y,s*.55,"0","#FFFDF5",ink)
    if name=="word_cards": return rect(x-72*s,y-34*s,45*s,68*s,accent,ink,3*s,7*s)+rect(x-20*s,y-34*s,45*s,68*s,secondary,ink,3*s,7*s)+rect(x+32*s,y-34*s,45*s,68*s,"#FFFDF5",ink,3*s,7*s)+text(x-49*s,y+10*s,"A",int(28*s),ink)+text(x+3*s,y+10*s,"B",int(28*s),ink)+text(x+55*s,y+10*s,"C",int(28*s),ink)
    if name=="number_line": return line(x-75*s,y,x+75*s,y,ink,5*s)+"".join(line(x-60*s+i*30*s,y-10*s,x-60*s+i*30*s,y+10*s,ink,3*s) for i in range(5))+circle(x+18*s,y,11*s,accent,ink,3*s)
    if name=="marker": return line(x-72*s,y,x+72*s,y,ink,5*s)+circle(x+25*s,y,15*s,accent,ink,4*s)+polygon(f"{x+25*s},{y-32*s} {x+11*s},{y-5*s} {x+39*s},{y-5*s}",accent,ink,3*s)
    if name=="rounding_arc": return line(x-70*s,y+25*s,x+70*s,y+25*s,ink,5*s)+path(f"M{x-55*s},{y+25*s} Q{x},{y-72*s} {x+55*s},{y+25*s}","none",accent,7*s)+circle(x-55*s,y+25*s,10*s,"#FFFDF5",ink,3*s)+circle(x+55*s,y+25*s,10*s,secondary,ink,3*s)
    if name=="plus": return line(x-28*s,y,x+28*s, y,ink,8*s)+line(x,y-28*s,x,y+28*s,ink,8*s)
    if name=="minus": return line(x-30*s,y,x+30*s,y,ink,8*s)
    if name=="times": return line(x-25*s,y-25*s,x+25*s,y+25*s,ink,8*s)+line(x+25*s,y-25*s,x-25*s,y+25*s,ink,8*s)
    if name=="fraction_circle" or name=="pie": return fraction_icon(x,y,s,accent,ink)
    if name=="containers" or name=="cup": return path(f"M{x-42*s},{y-45*s} L{x+42*s},{y-45*s} L{x+31*s},{y+50*s} L{x-31*s},{y+50*s} Z","#D9F3FF",ink,5*s)+path(f"M{x-30*s},{y+15*s} Q{x},{y-3*s} {x+30*s},{y+15*s} L{x+31*s},{y+50*s} L{x-31*s},{y+50*s} Z",accent,ink,3*s)
    if name=="water": return path(f"M{x},{y-55*s} Q{x-42*s},{y-4*s} {x-40*s},{y+20*s} Q{x-36*s},{y+55*s} {x},{y+55*s} Q{x+36*s},{y+55*s} {x+40*s},{y+20*s} Q{x+42*s},{y-4*s} {x},{y-55*s} Z",accent,ink,4*s)
    if name=="balance": return line(x,y-48*s,x,y+44*s,ink,5*s)+line(x-60*s,y+44*s,x+60*s,y+44*s,ink,5*s)+line(x-52*s,y-8*s,x+52*s,y-8*s,ink,5*s)+line(x-52*s,y-8*s,x-72*s,y+25*s,ink,4*s)+line(x+52*s,y-8*s,x+72*s,y+25*s,ink,4*s)+path(f"M{x-88*s},{y+25*s} Q{x-72*s},{y+48*s} {x-56*s},{y+25*s} Z",accent,ink,3*s)+path(f"M{x+56*s},{y+25*s} Q{x+72*s},{y+48*s} {x+88*s},{y+25*s} Z",secondary,ink,3*s)
    if name=="shapes": return circle(x-30*s,y-15*s,22*s,accent,ink,4*s)+rect(x+5*s,y-38*s,45*s,45*s,"#FFFDF5",ink,4*s,5*s)+polygon(f"{x-48*s},{y+48*s} {x-20*s},{y+1*s} {x+8*s},{y+48*s}",secondary,ink,4*s)
    if name=="grid": return rect(x-62*s,y-52*s,124*s,104*s,"#FFFDF5",ink,4*s,6*s)+"".join(line(x-31*s+i*31*s,y-50*s,x-31*s+i*31*s,y+50*s,secondary,3*s) for i in range(3))+"".join(line(x-60*s,y-26*s+i*26*s,x+60*s,y-26*s+i*26*s,secondary,3*s) for i in range(3))
    if name=="ruler": return rect(x-72*s,y-11*s,144*s,22*s,"#FFD66B",ink,4*s,5*s)+"".join(line(x-55*s+i*18*s,y-11*s,x-55*s+i*18*s,y+5*s,ink,3*s) for i in range(7))
    if name=="word_blocks" or name=="letter_blocks" or name=="letter_pair": return rect(x-58*s,y-34*s,50*s,68*s,accent,ink,4*s,7*s)+rect(x+8*s,y-34*s,50*s,68*s,secondary,ink,4*s,7*s)+text(x-33*s,y+11*s,"A",int(30*s),ink)+text(x+33*s,y+11*s,"B",int(30*s),ink)
    if name=="pencil": return polygon(f"{x-60*s},{y+20*s} {x+42*s},{y-40*s} {x+58*s},{y-22*s} {x-44*s},{y+40*s}","#FFD66B",ink,4*s)+polygon(f"{x+42*s},{y-40*s} {x+66*s},{y-58*s} {x+58*s},{y-22*s}","#F2B58F",ink,3*s)+line(x-24*s,y+10*s,x+34*s,y-26*s,accent,4*s)
    if name=="sentence" or name=="sentence_lines": return speech_lines(x,y,s,"#FFFDF5",ink)+circle(x+43*s,y+23*s,7*s,accent,ink,2*s)
    if name=="period": return circle(x,y+25*s,13*s,accent,ink,3*s)
    if name=="capital_a": return text(x,y+22*s,"A",int(86*s),accent)
    if name=="mouth": return ellipse(x,y,44*s,27*s,"#F2B58F",ink,4*s)+ellipse(x,y+3*s,27*s,9*s,"#B64D63",ink,3*s)
    if name=="sound" or name=="vibration": return polygon(f"{x-50*s},{y-17*s} {x-20*s},{y-17*s} {x+5*s},{y-42*s} {x+5*s},{y+42*s} {x-20*s},{y+17*s} {x-50*s},{y+17*s}",accent,ink,4*s)+path(f"M{x+24*s},{y-31*s} Q{x+57*s},{y} {x+24*s},{y+31*s}","none",ink,6*s)
    if name=="clap" or name=="hands": return path(f"M{x-50*s},{y+20*s} Q{x-44*s},{y-25*s} {x-10*s},{y-5*s} L{x-3*s},{y+20*s} Q{x-28*s},{y+12*s} {x-50*s},{y+20*s} Z",accent,ink,4*s)+path(f"M{x+50*s},{y+20*s} Q{x+44*s},{y-25*s} {x+10*s},{y-5*s} L{x+3*s},{y+20*s} Q{x+28*s},{y+12*s} {x+50*s},{y+20*s} Z",secondary,ink,4*s)+circle(x,y+5*s,13*s,"#FFD66B",ink,3*s)
    if name=="letter_cards" or name=="word_cards": return rect(x-70*s,y-34*s,42*s,68*s,accent,ink,3*s,6*s)+rect(x-21*s,y-34*s,42*s,68*s,secondary,ink,3*s,6*s)+rect(x+28*s,y-34*s,42*s,68*s,"#FFFDF5",ink,3*s,6*s)+text(x-49*s,y+10*s,"A",int(27*s),ink)+text(x,y+10*s,"B",int(27*s),ink)+text(x+49*s,y+10*s,"C",int(27*s),ink)
    if name=="arrow" or name=="link": return arrow(x,y,accent,ink,s)
    if name=="path": return path(f"M{x-75*s},{y+43*s} Q{x-27*s},{y-50*s} {x+18*s},{y+7*s} Q{x+44*s},{y+40*s} {x+76*s},{y-42*s}","none",accent,9*s)+circle(x-75*s,y+43*s,9*s,"#FFFDF5",ink,3*s)+circle(x+76*s,y-42*s,10*s,secondary,ink,3*s)
    if name=="magnifier": return circle(x-10*s,y-10*s,35*s,"#FFFDF5",ink,6*s)+line(x+16*s,y+17*s,x+53*s,y+54*s,ink,8*s)+circle(x-18*s,y-18*s,10*s,accent,ink,3*s)
    if name=="kite": return polygon(f"{x},{y-60*s} {x+40*s},{y} {x},{y+55*s} {x-40*s},{y}",accent,ink,4*s)+line(x,y+55*s,x+12*s,y+88*s,ink,3*s)+line(x+12*s,y+72*s,x+27*s,y+64*s,secondary,4*s)
    if name=="dog": return circle(x,y-16*s,31*s,"#C98755",ink,4*s)+circle(x-28*s,y-36*s,16*s,"#C98755",ink,4*s)+circle(x+28*s,y-36*s,16*s,"#C98755",ink,4*s)+circle(x-10*s,y-18*s,4*s,ink)+circle(x+10*s,y-18*s,4*s,ink)+ellipse(x,y+2*s,14*s,9*s,"#2E6F6D",ink,3*s)
    if name=="friendship": return person(x-28*s,y,s*.75,accent,"#F2B58F",ink,0)+person(x+28*s,y,s*.75,secondary,"#D98D68",ink,1)+heart(x,y-65*s,s*.4,accent,ink)
    if name=="diary": return book(x,y,s,accent,ink)+heart(x,y-3*s,s*.35,secondary,ink)
    if name=="calendar": return rect(x-55*s,y-45*s,110*s,95*s,"#FFFDF5",ink,4*s,7*s)+rect(x-55*s,y-45*s,110*s,23*s,accent,ink,3*s,7*s)+"".join(circle(x-30*s+c*30*s,y-4*s+r*24*s,4*s,secondary,ink,2*s) for r in range(2) for c in range(3))
    if name=="clock": return circle(x,y,48*s,"#FFFDF5",ink,4*s)+line(x,y,x,y-27*s,accent,5*s)+line(x,y,x+25*s,y+14*s,secondary,5*s)+circle(x,y,6*s,ink)
    if name=="counting" or name=="counters": return array_icon(x,y,s,accent,ink,3,4)
    if name=="boxes" or name=="basket": return rect(x-55*s,y-40*s,110*s,80*s,"#D9F3FF",ink,5*s,6*s)+line(x-55*s,y-15*s,x+55*s,y-15*s,ink,4*s)+line(x-38*s,y-40*s,x-38*s,y-15*s,ink,4*s)
    if name=="brush": return line(x-40*s,y+38*s,x+40*s,y-35*s,"#9A6A45",9*s)+path(f"M{x-46*s},{y+42*s} Q{x-68*s},{y+28*s} {x-53*s},{y+12*s} Q{x-35*s},{y+27*s} {x-46*s},{y+42*s} Z",accent,ink,3*s)
    if name=="one_many": return circle(x-34*s,y,12*s,accent,ink,3*s)+"".join(circle(x+20*s+(i%2)*25*s,y-17*s+(i//2)*25*s,9*s,secondary,ink,3*s) for i in range(4))
    if name=="word_twins": return rect(x-63*s,y-34*s,54*s,68*s,accent,ink,4*s,8*s)+rect(x+9*s,y-34*s,54*s,68*s,secondary,ink,4*s,8*s)+line(x-6*s,y,x+6*s,y,ink,6*s)+polygon(f"{x+2*s},{y-9*s} {x+18*s},{y} {x+2*s},{y+9*s}",ink,ink,2*s)
    if name=="bridge": return path(f"M{x-65*s},{y+35*s} Q{x},{y-45*s} {x+65*s},{y+35*s}","none",accent,8*s)+line(x-65*s,y+35*s,x-65*s,y+50*s,ink,5*s)+line(x+65*s,y+35*s,x+65*s,y+50*s,ink,5*s)
    if name=="word_cards": return rect(x-65*s,y-34*s,42*s,68*s,accent,ink,3*s,6*s)+rect(x-15*s,y-34*s,42*s,68*s,secondary,ink,3*s,6*s)+rect(x+35*s,y-34*s,42*s,68*s,"#FFFDF5",ink,3*s,6*s)+text(x-44*s,y+10*s,"A",int(25*s),ink)+text(x+6*s,y+10*s,"B",int(25*s),ink)+text(x+56*s,y+10*s,"C",int(25*s),ink)
    if name=="tree_root": return tree(x,y,s,accent,ink)+path(f"M{x},{y+68*s} Q{x-30*s},{y+85*s} {x-48*s},{y+110*s} M{x},{y+68*s} Q{x+30*s},{y+85*s} {x+48*s},{y+110*s}","none",ink,4*s)
    if name=="leaf": return path(f"M{x},{y+45*s} Q{x-55*s},{y+8*s} {x-22*s},{y-42*s} Q{x+32*s},{y-42*s} {x+44*s},{y-4*s} Q{x+31*s},{y+28*s} {x},{y+45*s} Z",accent,ink,5*s)+line(x-28*s,y+28*s,x+28*s,y-25*s,ink,4*s)
    if name=="recycle": return "".join(arrow(x+math.cos(i*2*math.pi/3)*35*s,y+math.sin(i*2*math.pi/3)*35*s,accent,ink,s*.45,angle=i*2*math.pi/3) for i in range(3))
    if name=="rock": return path(f"M{x-48*s},{y+35*s} L{x-30*s},{y-30*s} L{x+18*s},{y-48*s} L{x+52*s},{y+18*s} L{x+22*s},{y+48*s} Z",secondary,ink,4*s)
    if name=="metal": return rect(x-46*s,y-34*s,92*s,68*s,"#B8C7D1",ink,4*s,7*s)+line(x-20*s,y-5*s,x+20*s,y-5*s,"#FFFDF5",4*s)
    if name=="wood": return rect(x-52*s,y-24*s,104*s,48*s,"#B77B54",ink,4*s,8*s)+"".join(ellipse(x-25*s+i*25*s,y,9*s,4*s,"#8F573D", "none") for i in range(3))
    if name=="habitat": return tree(x-35*s,y,s*.6,accent,ink)+house(x+45*s,y+10*s,s*.5,secondary,ink)+sun(x+45*s,y-55*s,s*.35,secondary,ink)
    if name=="lab": return rect(x-38*s,y-52*s,76*s,104*s,"#FFFDF5",ink,4*s,7*s)+path(f"M{x-20*s},{y-28*s} L{x+20*s},{y-28*s} L{x+27*s},{y+38*s} L{x-27*s},{y+38*s} Z","#D9F3FF",ink,4*s)+path(f"M{x-24*s},{y+10*s} Q{x},{y-7*s} {x+24*s},{y+10*s} L{x+27*s},{y+38*s} L{x-27*s},{y+38*s} Z",accent,ink,3*s)
    if name=="rock" or name=="water": return rock_or_water(name,x,y,s,accent,ink)
    if name=="animal": return circle(x,y-20*s,27*s,secondary,ink,4*s)+circle(x-22*s,y-42*s,14*s,secondary,ink,4*s)+circle(x+22*s,y-42*s,14*s,secondary,ink,4*s)+ellipse(x,y+8*s,13*s,8*s,"#FFFDF5",ink,3*s)+line(x-24*s,y+8*s,x-24*s,y+46*s,ink,4*s)+line(x+24*s,y+8*s,x+24*s,y+46*s,ink,4*s)
    if name=="ramp": return polygon(f"{x-68*s},{y+43*s} {x+62*s},{y+43*s} {x+62*s},{y-37*s}","#D9F3FF",ink,4*s)+line(x-45*s,y+28*s,x+32*s,y-18*s,accent,7*s)
    if name=="ball": return circle(x,y,28*s,accent,ink,4*s)+path(f"M{x-22*s},{y-4*s} Q{x},{y-27*s} {x+22*s},{y-4*s}","none",ink,3*s)
    if name=="push": return person(x-42*s,y,s*.7,secondary,"#F2B58F",ink,0)+rect(x+12*s,y-34*s,52*s,68*s,accent,ink,4*s,5*s)+line(x-3*s,y-7*s,x+18*s,y-7*s,ink,5*s)+polygon(f"{x+18*s},{y-18*s} {x+35*s},{y-7*s} {x+18*s},{y+4*s}",ink,ink,2*s)
    if name=="surface": return line(x-70*s,y+30*s,x+70*s,y+30*s,ink,9*s)+line(x-70*s,y+48*s,x+70*s,y+48*s,secondary,7*s)+circle(x-32*s,y-5*s,21*s,accent,ink,4*s)
    if name=="lamp": return rect(x-12*s,y-46*s,24*s,75*s,accent,ink,4*s,5*s)+path(f"M{x-43*s},{y-47*s} Q{x},{y-92*s} {x+43*s},{y-47*s} L{x+30*s},{y-25*s} L{x-30*s},{y-25*s} Z","#FFD66B",ink,4*s)+line(x-36*s,y+35*s,x+36*s,y+35*s,ink,5*s)
    if name=="ear": return circle(x,y,38*s,"#FFFDF5",ink,4*s)+path(f"M{x+13*s},{y+21*s} Q{x+31*s},{y-12*s} {x+8*s},{y-28*s} Q{x-12*s},{y-42*s} {x-27*s},{y-19*s} Q{x-38*s},{y} {x-18*s},{y+20*s}","none",accent,8*s)
    if name=="lamp" or name=="sun": return sun(x,y,s,accent,ink)
    if name=="wind": return path(f"M{x-62*s},{y-20*s} Q{x-12*s},{y-47*s} {x+58*s},{y-20*s} M{x-50*s},{y+10*s} Q{x},{y-17*s} {x+43*s},{y+10*s}","none",secondary,6*s)
    if name=="weather_cards": return cloud(x-32*s,y,s*.55,"#FFFDF5",ink,True)+sun(x+48*s,y-15*s,s*.35,accent,ink)
    if name=="water" or name=="rain": return cloud(x,y-35*s,s*.65,"#FFFDF5",ink,True)
    if name=="lab": return lab(x,y,s,accent,ink)
    if name=="dictionary": return book(x,y,s,accent,ink)+magnifier(x+38*s,y-20*s,s*.45,accent,ink)
    if name=="checklist": return rect(x-45*s,y-58*s,90*s,116*s,"#FFFDF5",ink,4*s,8*s)+"".join(circle(x-25*s,y-28*s+i*30*s,6*s,accent,ink,2*s)+line(x-12*s,y-28*s+i*30*s,x+27*s,y-28*s+i*30*s,ink,4*s) for i in range(3))
    if name=="check": return path(f"M{x-42*s},{y} L{x-10*s},{y+32*s} L{x+53*s},{y-40*s}","none",accent,9*s)
    if name=="backpack": return rect(x-42*s,y-42*s,84*s,90*s,accent,ink,5*s,14*s)+line(x-25*s,y-42*s,x-25*s,y-70*s,ink,5*s)+line(x+25*s,y-42*s,x+25*s,y-70*s,ink,5*s)+rect(x-20*s,y-4*s,40*s,27*s,secondary,ink,3*s,5*s)
    if name=="clock": return clock(x,y,s,accent,ink)
    if name=="hand_wave": return hands(x,y,s,accent,ink)
    if name=="helper": return person(x,y,s,accent,"#F2B58F",ink,1)+star(x+42*s,y-57*s,s*.3,secondary,ink)
    if name=="lightbulb": return circle(x,y-14*s,30*s,"#FFD66B",ink,4*s)+line(x-12*s,y+16*s,x+12*s,y+16*s,ink,5*s)+line(x-9*s,y+28*s,x+9*s,y+28*s,ink,5*s)+line(x,y+35*s,x,y+50*s,ink,4*s)
    if name=="mirror": return rect(x-47*s,y-60*s,94*s,120*s,"#D9F3FF",ink,5*s,16*s)+person(x,y+15*s,s*.55,accent,"#F2B58F",ink,0)
    if name=="two_paths": return path(f"M{x},{y+55*s} Q{x-55*s},{y+15*s} {x-48*s},{y-45*s} M{x},{y+55*s} Q{x+55*s},{y+15*s} {x+48*s},{y-45*s}","none",accent,8*s)+flag(x-48*s,y-45*s,s*.35,accent,ink)+flag(x+48*s,y-45*s,s*.35,secondary,ink)
    if name=="old_new_house": return house(x-45*s,y,s*.68,"#B77B54",ink,True)+arrow(x+10*s,y+5*s,secondary,ink,s*.55)+house(x+68*s,y,s*.68,accent,ink)
    if name=="timeline": return line(x-70*s,y,x+70*s,y,ink,6*s)+"".join(circle(x+i*35*s,y,11*s,accent if i%2==0 else secondary,ink,3*s) for i in (-2,-1,0,1,2))+"".join(line(x+i*35*s,y-30*s,x+i*35*s,y-11*s,ink,3*s) for i in (-2,0,2))
    if name=="market": return rect(x-75*s,y-30*s,150*s,60*s,secondary,ink,4*s,6*s)+polygon(f"{x-85*s},{y-30*s} {x},{y-72*s} {x+85*s},{y-30*s}",accent,ink,4*s)+basket(x-35*s,y+10*s,s*.6,ink)+person(x+45*s,y+5*s,s*.55,secondary,"#F2B58F",ink,0)
    if name=="farm": return line(x-75*s,y+45*s,x+75*s,y+45*s,ink,5*s)+"".join(path(f"M{x+i*32*s},{y+42*s} Q{x+i*32*s-22*s},{y+7*s} {x+i*32*s-8*s},{y-22*s} Q{x+i*32*s+18*s},{y+5*s} {x+i*32*s},{y+42*s} Z",accent,ink,3*s) for i in (-2,-1,0,1,2))
    if name=="art": return rect(x-48*s,y-52*s,96*s,104*s,"#FFFDF5",ink,4*s,6*s)+path(f"M{x-34*s},{y+25*s} Q{x-8*s},{y-34*s} {x+8*s},{y+7*s} Q{x+26*s},{y-8*s} {x+35*s},{y+25*s} Z",accent,ink,3*s)+circle(x+18*s,y-23*s,9*s,"#FFD66B",ink,3*s)
    if name=="music": return sound(x,y,s,accent,ink)+"".join(circle(x+42*s+i*18*s,y-40*s-(i%2)*22*s,7*s,secondary,ink,3*s) for i in range(3))
    if name=="rhythm": return "".join(line(x-55*s+i*27*s,y+35*s,x-55*s+i*27*s,y-(15+i%3*22)*s,accent if i%2==0 else secondary,7*s) for i in range(5))
    if name=="seal": return circle(x,y,48*s,"#FFFDF5",ink,5*s)+circle(x,y,34*s,"none",accent,3*s)+star(x,y,s*.45,accent,ink)
    if name=="historic_house": return house(x,y,s,"#B77B54",ink,True)
    if name=="school": return house(x,y,s,accent,ink)+rect(x-15*s,y+20*s,30*s,25*s,secondary,ink,3*s,3*s)
    if name=="name_tag": return rect(x-62*s,y-28*s,124*s,56*s,"#FFFDF5",ink,4*s,9*s)+circle(x-40*s,y,9*s,accent,ink,3*s)+line(x-23*s,y,x+42*s,y,ink,4*s)
    if name=="open_box": return rect(x-55*s,y-35*s,110*s,70*s,"#FFFDF5",ink,4*s,5*s)+path(f"M{x-55*s},{y-35*s} L{x-20*s},{y-68*s} L{x+20*s},{y-35*s} M{x+55*s},{y-35*s} L{x+20*s},{y-68*s}",accent,ink,4*s)
    if name=="clock": return clock(x,y,s,accent,ink)
    if name=="question": return circle(x,y,43*s,"#FFFDF5",ink,4*s)+text(x,y+18*s,"?",int(55*s),accent)
    if name=="mirror": return rect(x-50*s,y-65*s,100*s,130*s,"#D9F3FF",ink,5*s,16*s)+person(x,y+16*s,s*.55,accent,"#F2B58F",ink,0)
    if name=="map" or name=="compass": return map_icon(x,y,s,accent,ink)
    if name=="route": return route(x,y,s,accent,ink)
    if name=="vibration": return sound(x,y,s,accent,ink)
    if name=="lab": return lab(x,y,s,accent,ink)
    if name=="recycle": return recycle(x,y,s,accent,ink)
    if name=="basket": return basket(x,y,s,ink)
    if name=="arrow": return arrow(x,y,accent,ink,s)
    if name=="star": return star(x,y,s,accent,ink)
    if name=="heart": return heart(x,y,s,accent,ink)
    return star(x,y,s,accent,ink)


def arrow(x: float,y: float,accent: str,ink: str,s: float=1.0,angle: float=0.0)->str:
    body=line(x-42*s,y,x+33*s,y,accent,7*s)+polygon(f"{x+24*s},{y-18*s} {x+55*s},{y} {x+24*s},{y+18*s}",accent,ink,3*s)
    if angle:
        body=tag("g",{"transform":f"rotate({angle*180/math.pi:.1f} {x} {y})"},body)
    return body


def heart(x: float,y: float,s: float,accent: str,ink: str)->str:
    return path(f"M{x},{y+45*s} C{x-75*s},{y-2*s} {x-44*s},{y-54*s} {x},{y-18*s} C{x+44*s},{y-54*s} {x+75*s},{y-2*s} {x},{y+45*s} Z",accent,ink,4*s)


def sound(x: float,y: float,s: float,accent: str,ink: str)->str:
    return draw_motif("sound",x,y,s,accent,"#FFD66B",ink,random.Random(0))


def hands(x: float,y: float,s: float,accent: str,ink: str)->str:
    return draw_motif("clap",x,y,s,accent,"#FFD66B",ink,random.Random(0))


def magnifier(x: float,y: float,s: float,accent: str,ink: str)->str:
    return circle(x-10*s,y-10*s,35*s,"#FFFDF5",ink,6*s)+line(x+16*s,y+17*s,x+53*s,y+54*s,ink,8*s)+circle(x-18*s,y-18*s,10*s,accent,ink,3*s)


def basket(x: float,y: float,s: float,ink: str)->str:
    return path(f"M{x-55*s},{y-30*s} L{x+55*s},{y-30*s} L{x+40*s},{y+43*s} L{x-40*s},{y+43*s} Z","#D99B57",ink,4*s)+path(f"M{x-36*s},{y-30*s} Q{x},{y-85*s} {x+36*s},{y-30*s}","none",ink,5*s)


def recycle(x: float,y: float,s: float,accent: str,ink: str)->str:
    out=""
    for i in range(3): out+=arrow(x+math.cos(i*2*math.pi/3)*28*s,y+math.sin(i*2*math.pi/3)*28*s,accent,ink,s*.42,angle=i*2*math.pi/3)
    return out


def route(x: float,y: float,s: float,accent: str,ink: str)->str:
    return line(x-65*s,y+35*s,x-20*s,y-12*s,accent,7*s)+line(x-20*s,y-12*s,x+22*s,y+20*s,accent,7*s)+line(x+22*s,y+20*s,x+62*s,y-40*s,accent,7*s)+circle(x-65*s,y+35*s,9*s,"#FFFDF5",ink,3*s)+circle(x+62*s,y-40*s,11*s,"#FFD66B",ink,3*s)


def lab(x: float,y: float,s: float,accent: str,ink: str)->str:
    return rect(x-38*s,y-52*s,76*s,104*s,"#FFFDF5",ink,4*s,7*s)+path(f"M{x-20*s},{y-28*s} L{x+20*s},{y-28*s} L{x+27*s},{y+38*s} L{x-27*s},{y+38*s} Z","#D9F3FF",ink,4*s)+path(f"M{x-24*s},{y+10*s} Q{x},{y-7*s} {x+24*s},{y+10*s} L{x+27*s},{y+38*s} L{x-27*s},{y+38*s} Z",accent,ink,3*s)


def rock_or_water(name: str,x: float,y: float,s: float,accent: str,ink: str)->str:
    if name=="rock": return path(f"M{x-48*s},{y+35*s} L{x-30*s},{y-30*s} L{x+18*s},{y-48*s} L{x+52*s},{y+18*s} L{x+22*s},{y+48*s} Z",accent,ink,4*s)
    return path(f"M{x},{y-55*s} Q{x-42*s},{y-4*s} {x-40*s},{y+20*s} Q{x-36*s},{y+55*s} {x},{y+55*s} Q{x+36*s},{y+55*s} {x+40*s},{y+20*s} Q{x+42*s},{y-4*s} {x},{y-55*s} Z",accent,ink,4*s)


def topic_key(lesson: dict[str, object]) -> str:
    subject = canonical_subject(str(lesson.get("subject", "")))
    key = str(profile_for(lesson).get("key", "general"))
    if key == "general":
        return {"english": "general_english", "mathematics": "general_math", "science": "general_science", "filipino": "general_filipino", "gmrc": "general_gmrc", "makabansa": "community_history", "araling-panlipunan": "general_ap"}.get(subject, "general_english")
    if key in {"greeting", "polite"}:
        return "polite"
    if key == "root" and subject == "filipino": return "root_filipino"
    return key


def scene_svg(lesson: dict[str, object]) -> str:
    lesson_id = str(lesson.get("lessonId", "lesson"))
    subject = canonical_subject(str(lesson.get("subject", "")))
    key = topic_key(lesson)
    bg, accent, ink, secondary = PALETTES.get(subject, PALETTES["english"])
    rng = stable_rng(lesson_id)
    motifs = list(MOTIFS.get(key, MOTIFS.get("general_" + subject, MOTIFS["general_english"])))
    rng.shuffle(motifs)
    chosen = motifs[:5]
    variant = rng.randrange(5)
    # Keep the scene uncluttered, but make the composition and visual anchor
    # change between lessons instead of reusing a card grid.
    sky = bg
    ground = "#D9EFC7" if subject in {"science", "makabansa", "araling-panlipunan"} else "#F8DDB0"
    out = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="800" height="450" viewBox="0 0 800 450" role="img" aria-labelledby="title desc">',
        f'<title id="title">{esc(lesson.get("title", lesson_id))} illustration</title>',
        f'<desc id="desc">A topic-specific illustrated scene for {esc(lesson.get("title", lesson_id))}. It shows visual clues for the lesson objective.</desc>',
        '<defs>',
        f'<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{sky}"/><stop offset="1" stop-color="#FFFFFF" stop-opacity="0.72"/></linearGradient>',
        '</defs>',
        '<rect width="800" height="450" rx="38" fill="url(#sky)"/>',
    ]
    # Background varies by subject and seed.
    out.append(sun(92 + (rng.randrange(3) * 35), 72, .65, secondary, ink))
    if variant in (1, 4): out.append(cloud(650, 70, .65, "#FFFDF5", ink, variant == 4))
    if subject in {"science", "araling-panlipunan", "makabansa"}:
        out.append(path("M0 300 Q140 230 290 302 T570 290 T800 304 L800 450 L0 450 Z", "#B7DFAE", "#578B55", 3))
    else:
        out.append(path("M0 320 Q170 240 330 315 T620 300 T800 320 L800 450 L0 450 Z", "#CFE6C4", "#7AA46A", 3))
    out.append(rect(0, 340, 800, 110, ground))
    # Draw the central semantic anchor large, then supporting clues around it.
    anchor = chosen[0]
    placements = {
        0: [(410, 265, 1.25), (155, 250, .58), (660, 255, .60), (250, 365, .48), (570, 365, .48)],
        1: [(395, 230, 1.15), (130, 150, .55), (660, 160, .55), (220, 365, .52), (630, 365, .52)],
        2: [(260, 260, 1.10), (545, 235, .82), (125, 370, .48), (405, 370, .48), (685, 365, .48)],
        3: [(520, 245, 1.18), (160, 230, .58), (300, 365, .46), (650, 145, .50), (690, 365, .48)],
        4: [(400, 295, 1.12), (130, 270, .55), (680, 270, .55), (270, 140, .48), (600, 140, .48)],
    }[variant]
    for i, (name, (x, y, scale)) in enumerate(zip(chosen, placements)):
        out.append(draw_motif(name, x, y, scale, accent if i % 2 == 0 else secondary, secondary if i % 2 == 0 else accent, ink, rng))
    # A small lesson-specific visual signature prevents accidental convergence
    # while remaining decorative: topic color dots and a seed-based pennant.
    sig = int(hashlib.sha256(lesson_id.encode()).hexdigest()[-4:], 16)
    for i in range(3 + sig % 4):
        x = 42 + ((sig + i * 83) % 710)
        y = 110 + ((sig // 7 + i * 47) % 105)
        out.append(circle(x, y, 4 + (sig + i) % 5, accent if i % 2 else secondary, "none"))
    out.append(flag(735, 345, .42, accent, ink))
    out.append('</svg>\n')
    return "".join(out)


# Backwards-compatible alias for callers that prefer a generator-style name.
bespoke_svg = scene_svg
