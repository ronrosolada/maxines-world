#!/usr/bin/env python3
"""
Maxine's World - Generative Content & Audio Engine CLI
Command-line interface to author, illustrate, narrate, and package modular educational content.
"""

import sys
import argparse
import logging
from pathlib import Path

from .audio_synthesizer import AudioSynthesizer
from .svg_generator import SvgAssetGenerator
from .lesson_author import LessonAuthor, VocabularyItem, ActivityStep, AssessmentItem
from .packager_validator import ContentPackager

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("ContentEngineCLI")

def run_pilot_build(output_dir: Path):
    """
    Builds an end-to-end pilot Science lesson with Milo narration and vector assets.
    """
    logger.info("Initializing Content & Audio Engine...")
    synthesizer = AudioSynthesizer()
    svg_gen = SvgAssetGenerator()
    author = LessonAuthor()
    packager = ContentPackager(output_dir)

    build_temp = output_dir / "temp"
    audio_temp = build_temp / "audio"
    svg_temp = build_temp / "assets"
    audio_temp.mkdir(parents=True, exist_ok=True)
    svg_temp.mkdir(parents=True, exist_ok=True)

    # 1. Author Lesson
    logger.info("Step 1: Authoring structured Grade 3 Science lesson...")
    vocab = [
        VocabularyItem("Organism", "Any living biological entity such as a plant or animal."),
        VocabularyItem("Nutrient", "A substance essential for growth and life."),
        VocabularyItem("Habitat", "The natural home of a plant or animal.")
    ]

    act1 = ActivityStep(
        activityId="sci-g3-q1-w01-d01-a01",
        type="ANIMATED_EXPLANATION_V1",
        instruction="Listen to Milo explain living things.",
        prompt="Living things grow, breathe, and reproduce.",
        narration="Look at the garden! Trees grow from tiny seeds, birds sing in the branches, and kittens play. All living things need food and water to grow.",
        guideHint="Notice how plants and animals change and grow over time!",
        content={
            "explanationType": "DEMONSTRATION",
            "body": "Living organisms need food, air, and water."
        },
        completionRule="VIEW_AND_ACKNOWLEDGE",
        assetId="sci-g3-q1-w01-d01-board.svg"
    )

    act2 = ActivityStep(
        activityId="sci-g3-q1-w01-d01-a02",
        type="SORT_AND_CLASSIFY_V1",
        instruction="Sort the items into Living and Non-Living.",
        prompt="Drag each card to the right basket.",
        narration="Can you help me sort these items? Put living things in the green basket and non-living objects in the blue basket.",
        guideHint="Ask yourself: Does it breathe or grow?",
        content={
            "categoryA": "Living Things",
            "categoryB": "Non-Living Things",
            "items": [
                {"name": "Sunflower", "category": "A"},
                {"name": "Wooden Chair", "category": "B"},
                {"name": "Puppy", "category": "A"},
                {"name": "Bicycle", "category": "B"}
            ]
        },
        completionRule="ALL_ITEMS_SORTED",
        assetId="sci-g3-q1-w01-d01-board.svg"
    )

    assessments = [
        AssessmentItem(
            itemId="q1",
            prompt="Which of the following is a living thing?",
            choices=["A growing mango tree", "A plastic toy car", "A ceramic coffee mug", "A metal spoon"],
            correctIndex=0,
            feedback={"correct": "Great job! Mango trees grow and need sunlight.", "retry": "Think about what needs water and sunlight to grow."}
        ),
        AssessmentItem(
            itemId="q2",
            prompt="What do animals need to survive?",
            choices=["Air, water, and food", "Batteries and wires", "Paint and brushes", "Gasoline and motor oil"],
            correctIndex=0,
            feedback={"correct": "Spot on! Living things need nourishment and air.", "retry": "Living things need biological nourishment."}
        ),
        AssessmentItem(
            itemId="q3",
            prompt="Why is a stone considered a non-living thing?",
            choices=["It does not grow, eat, or breathe", "It is found outside in the soil", "It can roll down a steep hill", "It is very hard and heavy"],
            correctIndex=0,
            feedback={"correct": "Exactly! Non-living objects do not perform life processes.", "retry": "Remember the key traits of living organisms."}
        ),
        AssessmentItem(
            itemId="q4",
            prompt="Which organism makes its own food using sunlight?",
            choices=["A green fern plant", "A hungry honeybee", "A playful puppy dog", "A chirping sparrow"],
            correctIndex=0,
            feedback={"correct": "Excellent! Green plants produce food through photosynthesis.", "retry": "Which organism has green leaves that absorb sunlight?"}
        ),
        AssessmentItem(
            itemId="q5",
            prompt="What will happen to a potted plant if it gets no water or sunlight?",
            choices=["It will stop growing and wither", "It will turn into a rock", "It will run away to find a pond", "It will stay green forever"],
            correctIndex=0,
            feedback={"correct": "Correct! Living plants need water and sunlight to survive.", "retry": "Living organisms require essential nutrients to live."}
        )
    ]

    lesson = author.create_lesson(
        lesson_id="science-g3-q1-w01-d01-living-things",
        title="Living and Non-Living Things",
        subject="SCIENCE",
        grade=3,
        quarter=1,
        week=1,
        day=1,
        objective="Identify the characteristics distinguishing living organisms from non-living objects.",
        competency_code="S3LT-IIa-b-1",
        introduction="Welcome Batang Matalino! Today, Milo the Cat will help us explore living and non-living things in our environment.",
        story_intro="Milo is taking a morning walk in the garden. Let's see what is alive and what is not!",
        vocabulary=vocab,
        activities=[act1, act2],
        assessment_items=assessments
    )

    # 2. Validate Lesson
    logger.info("Step 2: Validating lesson structure & quality gates...")
    valid, errors = packager.validate_lesson(lesson)
    if not valid:
        logger.error(f"Validation failed: {errors}")
        sys.exit(1)
    logger.info("Validation passed with 0 errors!")

    # 3. Generate Audio
    logger.info("Step 3: Synthesizing offline Piper TTS audio (OGG/Opus)...")
    audio_map = synthesizer.batch_synthesize_lesson(lesson, audio_temp)
    audio_files = list(audio_temp.glob("*.ogg"))
    logger.info(f"Synthesized {len(audio_files)} audio prompts successfully.")

    # 4. Generate SVG Boards
    logger.info("Step 4: Rendering canonical Milo 640x360 SVG board...")
    board_path = svg_temp / "sci-g3-q1-w01-d01-board.svg"
    visuals = [
        {"type": "card", "x": 140, "y": 110, "label": "Plant (Living)"},
        {"type": "card", "x": 300, "y": 110, "label": "Puppy (Living)"},
        {"type": "card", "x": 460, "y": 110, "label": "Chair (Non-living)"}
    ]
    svg_gen.generate_activity_board(
        title="Living and Non-Living Things",
        subject="SCIENCE",
        topic_visuals=visuals,
        output_svg_path=board_path,
        instruction="Explore the items in the garden with Milo!"
    )
    svg_files = [board_path]

    # 5. Package Module
    logger.info("Step 5: Packaging into immutable SHA-256 ZIP bundle...")
    catalog_entry = packager.package_module(
        package_id="ph-matatag-g3-science-q1-w01",
        version="1.0.0",
        title="Grade 3 Science: Living Things",
        subject="SCIENCE",
        grade=3,
        lessons=[lesson],
        asset_files=svg_files,
        audio_files=audio_files
    )

    logger.info("=" * 60)
    logger.info("Module Packaging Complete!")
    logger.info(f"Package ID:   {catalog_entry['packageId']}")
    logger.info(f"Package SHA:  {catalog_entry['sha256']}")
    logger.info(f"Package Size: {catalog_entry['sizeBytes']} bytes ({catalog_entry['sizeBytes']/1024:.1f} KB)")
    logger.info(f"Catalog URL:  {output_dir / 'catalog.json'}")
    logger.info("=" * 60)

def main():
    parser = argparse.ArgumentParser(description="Maxine's World Generative Content & Audio Engine")
    parser.add_argument("--output-dir", type=Path, default=Path("/home/ron/workspace/maxines-world/build/content_output"), help="Output build directory")
    parser.add_argument("--demo", action="store_true", help="Run end-to-end demo build")
    
    args = parser.parse_args()
    if args.demo:
        run_pilot_build(args.output_dir)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
