#!/usr/bin/env python3
"""
Maxine's World - Enhanced Illustrated SVG Asset Generator
Produces high-quality, engaging, subject-thematic 640x360 SVG activity boards with:
- Authentic Milo character anchor (matching master APK launcher icon)
- Subject-themed background environments (Forest, Laboratory, Classroom, Village, Math Grid)
- Rich illustrated topic visual cards with crisp vector icons
- Interactive calibrated touch hotspots
"""

import base64
import html
from pathlib import Path
from typing import Dict, List, Optional

MASTER_MILO_ICON = Path("/home/ron/workspace/maxines-world/tools/content_engine/milo_icon_anchor.png")

# Enhanced subject color palettes and environmental themes
THEMES = {
    "SCIENCE": {
        "bg_top": "#E8F5E9",
        "bg_bottom": "#C8E6C9",
        "accent": "#2E7D32",
        "badge": "#1B5E20",
        "card_border": "#81C784",
        "tag": "🌿 Science &amp; Nature",
        "decor": "leaves"
    },
    "MATHEMATICS": {
        "bg_top": "#E1F5FE",
        "bg_bottom": "#B3E5FC",
        "accent": "#0288D1",
        "badge": "#01579B",
        "card_border": "#4FC3F7",
        "tag": "🔢 Math Adventure",
        "decor": "grid"
    },
    "ENGLISH": {
        "bg_top": "#FFF8E1",
        "bg_bottom": "#FFECB3",
        "accent": "#F57C00",
        "badge": "#E65100",
        "card_border": "#FFB74D",
        "tag": "📚 Reading &amp; Stories",
        "decor": "books"
    },
    "FILIPINO": {
        "bg_top": "#FBE9E7",
        "bg_bottom": "#FFCCBC",
        "accent": "#D84315",
        "badge": "#BF360C",
        "card_border": "#FF8A65",
        "tag": "🌸 Wika at Panitikan",
        "decor": "sun"
    },
    "MAKABANSA": {
        "bg_top": "#F3E5F5",
        "bg_bottom": "#E1BEE7",
        "accent": "#7B1FA2",
        "badge": "#4A148C",
        "card_border": "#BA68C8",
        "tag": "🏛️ Kultura at Komunidad",
        "decor": "islands"
    },
    "ARALING": {
        "bg_top": "#F3E5F5",
        "bg_bottom": "#E1BEE7",
        "accent": "#7B1FA2",
        "badge": "#4A148C",
        "card_border": "#BA68C8",
        "tag": "🗺️ Araling Panlipunan",
        "decor": "map"
    },
    "GMRC": {
        "bg_top": "#FFFDE7",
        "bg_bottom": "#FFF9C4",
        "accent": "#FBC02D",
        "badge": "#F57F17",
        "card_border": "#FFF176",
        "tag": "💛 Kabutihang Asal",
        "decor": "heart"
    }
}

class SvgAssetGenerator:
    def __init__(self):
        self.width = 640
        self.height = 360
        self._milo_b64: Optional[str] = None
        self._load_master_icon()

    def _load_master_icon(self):
        if MASTER_MILO_ICON.exists():
            with open(MASTER_MILO_ICON, "rb") as f:
                self._milo_b64 = f"data:image/png;base64,{base64.b64encode(f.read()).decode('utf-8')}"

    def generate_activity_board(
        self,
        title: str,
        subject: str,
        topic_visuals: List[Dict],
        output_svg_path: Path,
        instruction: Optional[str] = None,
        hotspots: Optional[List[Dict]] = None
    ) -> Path:
        """
        Generates a rich, engaging 640x360 SVG board for Maxine's World activities.
        """
        output_svg_path.parent.mkdir(parents=True, exist_ok=True)
        
        subj_key = subject.upper()
        theme = THEMES.get(subj_key, THEMES.get(subj_key.split("-")[0], THEMES["SCIENCE"]))
        
        # Hotspot Touch Targets
        hotspot_elements = []
        if hotspots:
            for idx, hs in enumerate(hotspots):
                hx = int(hs.get("x", 0.5) * self.width)
                hy = int(hs.get("y", 0.5) * self.height)
                hs_xml = f"""
                <g class="hotspot-target" transform="translate({hx}, {hy})" filter="drop-shadow(0 2px 4px rgba(0,0,0,0.18))">
                  <circle r="18" fill="#FFD54F" stroke="#F57F17" stroke-width="3"/>
                  <circle r="6" fill="#D84315"/>
                  <text x="0" y="5" font-family="system-ui, -apple-system, sans-serif" font-size="12" font-weight="900" fill="#3E2723" text-anchor="middle">{idx+1}</text>
                </g>
                """
                hotspot_elements.append(hs_xml)

        # Illustrated Topic Cards & Diagrams
        visual_elements = []
        n_cards = len(topic_visuals)
        for idx, vis in enumerate(topic_visuals):
            # Calculate automatic balanced positions if not provided
            if "x" in vis and "y" in vis:
                vx, vy = vis["x"], vis["y"]
            else:
                card_width = 150
                gap = (self.width - 160 - (n_cards * card_width)) / (n_cards + 1)
                vx = int(140 + gap + idx * (card_width + gap))
                vy = 120

            v_type = vis.get("type", "card")
            v_label = html.escape(str(vis.get("label", "")))
            v_desc = html.escape(str(vis.get("desc", "")))
            v_icon = html.escape(str(vis.get("icon", "✨")))
            
            card_xml = f"""
            <g transform="translate({vx}, {vy})" filter="drop-shadow(0 6px 12px rgba(0,0,0,0.07))">
              <!-- Card Background -->
              <rect width="145" height="150" rx="16" fill="#FFFFFF" stroke="{theme['card_border']}" stroke-width="2.5"/>
              
              <!-- Icon Container -->
              <circle cx="72.5" cy="48" r="28" fill="{theme['bg_top']}" stroke="{theme['accent']}" stroke-width="1.5"/>
              <text x="72.5" y="56" font-family="system-ui, -apple-system, sans-serif" font-size="24" text-anchor="middle">{v_icon}</text>
              
              <!-- Card Label -->
              <text x="72.5" y="98" font-family="system-ui, -apple-system, sans-serif" font-size="13" font-weight="bold" fill="#1E293B" text-anchor="middle">{v_label}</text>
              
              <!-- Card Sub-description -->
              <text x="72.5" y="120" font-family="system-ui, -apple-system, sans-serif" font-size="10" font-weight="500" fill="#64748B" text-anchor="middle">{v_desc}</text>
              
              <!-- Bottom Interactive Bar -->
              <rect x="25" y="132" width="95" height="4" rx="2" fill="{theme['accent']}" opacity="0.6"/>
            </g>
            """
            visual_elements.append(card_xml)

        # Milo Avatar Guide Badge (Mirroring APK master launcher icon)
        milo_anchor = f"""
        <!-- Master APK Milo Guide Avatar Anchor -->
        <g id="milo-guide-anchor" transform="translate(18, 12)">
          <!-- Outer Shadowed Badge -->
          <circle cx="46" cy="46" r="44" fill="#FFFFFF" stroke="{theme['accent']}" stroke-width="4" filter="drop-shadow(0 4px 8px rgba(0,0,0,0.15))"/>
          <clipPath id="miloCircleClip">
            <circle cx="46" cy="46" r="41"/>
          </clipPath>
          <image href="{self._milo_b64}" x="5" y="5" width="82" height="82" clip-path="url(#miloCircleClip)" preserveAspectRatio="xMidYMid slice"/>
        </g>
        """

        esc_instruction = html.escape(str(instruction or title))

        svg_content = f"""<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 {self.width} {self.height}" width="{self.width}" height="{self.height}">
  <defs>
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="{theme['bg_top']}"/>
      <stop offset="100%" stop-color="{theme['bg_bottom']}"/>
    </linearGradient>
    <filter id="softGlow" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="3" result="blur" />
      <feComposite in="SourceGraphic" in2="blur" operator="over" />
    </filter>
  </defs>

  <!-- Background Environment -->
  <rect width="{self.width}" height="{self.height}" rx="20" fill="url(#bgGrad)"/>
  
  <!-- Subtle Environment Patterns -->
  <g opacity="0.08" fill="{theme['accent']}">
    <circle cx="80" cy="280" r="120"/>
    <circle cx="560" cy="80" r="100"/>
    <circle cx="520" cy="320" r="80"/>
  </g>
  
  <!-- Content Frame -->
  <rect x="10" y="10" width="{self.width - 20}" height="{self.height - 20}" rx="16" fill="none" stroke="{theme['accent']}" stroke-width="2" opacity="0.25"/>

  <!-- Top Banner / Subject Badge & Instruction -->
  <g id="header-banner" transform="translate(120, 18)">
    <rect width="{self.width - 140}" height="64" rx="20" fill="#FFFFFF" opacity="0.97" stroke="{theme['card_border']}" stroke-width="2" filter="drop-shadow(0 2px 6px rgba(0,0,0,0.06))"/>
    
    <!-- Subject Tag -->
    <rect x="16" y="10" width="140" height="20" rx="10" fill="{theme['bg_top']}"/>
    <text x="24" y="24" font-family="system-ui, -apple-system, sans-serif" font-size="10" font-weight="800" fill="{theme['accent']}">{theme['tag']}</text>
    
    <!-- Instruction Prompt -->
    <text x="16" y="48" font-family="system-ui, -apple-system, sans-serif" font-size="14" font-weight="bold" fill="#1E293B">
      {esc_instruction}
    </text>
  </g>

  <!-- Milo Avatar Guide Anchor -->
  {milo_anchor}

  <!-- Interactive Activity Content -->
  <g id="activity-visuals">
    {''.join(visual_elements)}
  </g>

  <!-- Interactive Hotspots -->
  <g id="hotspots">
    {''.join(hotspot_elements)}
  </g>

  <!-- Bottom Hint & Help Indicator -->
  <g opacity="0.8" transform="translate({self.width - 45}, {self.height - 40})">
    <circle cx="16" cy="16" r="16" fill="#FFFFFF" stroke="{theme['accent']}" stroke-width="2" filter="drop-shadow(0 2px 4px rgba(0,0,0,0.1))"/>
    <text x="16" y="21" font-family="system-ui, -apple-system, sans-serif" font-size="14" font-weight="900" fill="{theme['accent']}" text-anchor="middle">💡</text>
  </g>
</svg>
"""
        with open(output_svg_path, "w", encoding="utf-8") as f:
            f.write(svg_content.strip())
            
        return output_svg_path

if __name__ == "__main__":
    gen = SvgAssetGenerator()
    test_svg = Path("/home/ron/workspace/test_enhanced_board.svg")
    visuals = [
        {"icon": "🌱", "label": "Living Organism", "desc": "Grows & Drinks"},
        {"icon": "🐕", "label": "Playful Puppy", "desc": "Breathes & Moves"},
        {"icon": "🪨", "label": "River Stone", "desc": "Non-Living Object"},
    ]
    hotspots = [
        {"x": 0.33, "y": 0.72},
        {"x": 0.58, "y": 0.72},
        {"x": 0.83, "y": 0.72}
    ]
    gen.generate_activity_board(
        title="Science Grade 3: Living Things",
        subject="SCIENCE",
        topic_visuals=visuals,
        output_svg_path=test_svg,
        instruction="Tap each card to discover why plants and animals are alive!",
        hotspots=hotspots
    )
    print("Enhanced SVG board generated successfully!")
