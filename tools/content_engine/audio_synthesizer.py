#!/usr/bin/env python3
"""
Maxine's World - Audio Narration Engine (Piper TTS + Opus Compression)
Generates crystal-clear, offline-ready audio narration for Milo & lesson prompts.
"""

import os
import sys
import wave
import hashlib
import logging
import subprocess
from pathlib import Path
from typing import Optional, List, Dict

try:
    from piper import PiperVoice
except ImportError:
    PiperVoice = None

logger = logging.getLogger("AudioSynthesizer")

DEFAULT_MODEL_DIR = Path(__file__).parent.parent.parent / "voice_models"
DEFAULT_VOICE_EN = "en_US-amy-medium.onnx"

class AudioSynthesizer:
    def __init__(self, model_path: Optional[str] = None, model_dir: Optional[str] = None):
        self.model_dir = Path(model_dir) if model_dir else DEFAULT_MODEL_DIR
        self.model_path = Path(model_path) if model_path else (self.model_dir / DEFAULT_VOICE_EN)
        self.config_path = Path(str(self.model_path) + ".json")
        self._voice: Optional[PiperVoice] = None

    def _ensure_voice_loaded(self):
        if self._voice is not None:
            return
        if not self.model_path.exists():
            raise FileNotFoundError(f"Voice model not found at: {self.model_path}. Please download voice models first.")
        if PiperVoice is None:
            raise ImportError("piper-tts is not installed. Install via pip install piper-tts.")
        
        self._voice = PiperVoice.load(str(self.model_path), str(self.config_path))

    def synthesize_text(self, text: str, output_ogg_path: str | Path, bit_rate: str = "32k") -> Path:
        """
        Synthesizes text prompt into an OGG/Opus compressed audio file.
        """
        self._ensure_voice_loaded()
        out_path = Path(output_ogg_path)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        
        temp_wav = out_path.with_suffix(".tmp.wav")
        try:
            with wave.open(str(temp_wav), "wb") as wav_file:
                self._voice.synthesize_wav(text, wav_file)
            
            # Convert WAV to high-efficiency OGG/Opus
            cmd = [
                "ffmpeg", "-y", "-i", str(temp_wav),
                "-c:a", "libopus", "-b:a", bit_rate,
                "-v", "error",
                str(out_path)
            ]
            subprocess.run(cmd, check=True)
            return out_path
        finally:
            if temp_wav.exists():
                temp_wav.unlink()

    def batch_synthesize_lesson(self, lesson_data: Dict, output_audio_dir: Path) -> Dict[str, str]:
        """
        Synthesizes all narration and prompts for a lesson structure, returning a mapping of field -> audio file path.
        """
        audio_map = {}
        output_audio_dir.mkdir(parents=True, exist_ok=True)
        
        # 1. Lesson Intro
        if "introduction" in lesson_data and lesson_data["introduction"]:
            fn = "intro.ogg"
            self.synthesize_text(lesson_data["introduction"], output_audio_dir / fn)
            audio_map["introduction"] = fn

        if "storyIntro" in lesson_data and lesson_data["storyIntro"]:
            fn = "story_intro.ogg"
            self.synthesize_text(lesson_data["storyIntro"], output_audio_dir / fn)
            audio_map["storyIntro"] = fn

        # 2. Activities
        for idx, act in enumerate(lesson_data.get("activities", [])):
            act_id = act.get("activityId", f"a{idx+1:02d}")
            
            # Narration
            if "narration" in act and act["narration"]:
                fn = f"{act_id}_narration.ogg"
                self.synthesize_text(act["narration"], output_audio_dir / fn)
                audio_map[f"{act_id}.narration"] = fn
            
            # Instruction prompt
            if "prompt" in act and act["prompt"]:
                fn = f"{act_id}_prompt.ogg"
                self.synthesize_text(act["prompt"], output_audio_dir / fn)
                audio_map[f"{act_id}.prompt"] = fn
                
            # Guide hint
            if "guideHint" in act and act["guideHint"]:
                fn = f"{act_id}_hint.ogg"
                self.synthesize_text(act["guideHint"], output_audio_dir / fn)
                audio_map[f"{act_id}.guideHint"] = fn

        return audio_map

if __name__ == "__main__":
    synthesizer = AudioSynthesizer()
    test_out = Path("/tmp/milo_sample.ogg")
    res = synthesizer.synthesize_text("Hi there! Milo here. Let's do some fun learning!", test_out)
    print(f"Generated sample audio at: {res} ({res.stat().st_size} bytes)")
