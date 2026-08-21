// Milo — Maxine's World mascot reward states
// 4 states: idle (neutral), anticipate, celebrate (star), calm
// Shared ids use identical command structure across states -> TRANSFORM (perfect bezier lerp)
// Celebrate sparkles are orphans that fade in/out.

window.PREVIEW_CONFIG = {
  title: "Milo — Reward Celebration",
  subtitle: "Maxine's World · hero sticker reveal prototype · drag the curve + test reduced-motion",
  viewBox: "0 0 320 320",
  size: { width: 320, height: 320 },
};

window.STATES_DATA = {
  "idle": {
    paths: {
      // shadow on ground
      shadow: { tag: "ellipse", cx: "160", cy: "278", rx: "54", ry: "10", fill: "#0d0d0f14", stroke: "none" },
      // tail
      tail: { tag: "ellipse", cx: "104", cy: "222", rx: "18", ry: "26", fill: "#F5B82E", stroke: "#7A3B00", strokeWidth: "3" },
      tailTip: { tag: "ellipse", cx: "92", cy: "212", rx: "10", ry: "9", fill: "#FFFEF8", stroke: "none" },
      // body
      body: { tag: "ellipse", cx: "160", cy: "210", rx: "58", ry: "60", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3.2" },
      belly: { tag: "ellipse", cx: "160", cy: "228", rx: "36", ry: "28", fill: "#FFFEF8", stroke: "none" },
      // arms down
      armL: { tag: "path", d: "M 108 198 C 92 212 90 238 108 252 C 118 258 128 252 126 236 L 120 204 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinecap: "round", strokeLinejoin: "round" },
      armR: { tag: "path", d: "M 212 198 C 228 212 230 238 212 252 C 202 258 192 252 194 236 L 200 204 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinecap: "round", strokeLinejoin: "round" },
      // head
      head: { tag: "ellipse", cx: "160", cy: "124", rx: "62", ry: "56", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3.2" },
      cheekL: { tag: "ellipse", cx: "128", cy: "142", rx: "16", ry: "11", fill: "#F47C6B", stroke: "none", opacity: "0.18" },
      cheekR: { tag: "ellipse", cx: "192", cy: "142", rx: "16", ry: "11", fill: "#F47C6B", stroke: "none", opacity: "0.18" },
      earL: { tag: "path", d: "M 112 88 C 98 62 118 52 134 74 L 124 102 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinejoin: "round" },
      earR: { tag: "path", d: "M 208 88 C 222 62 202 52 186 74 L 196 102 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinejoin: "round" },
      earInnerL: { tag: "path", d: "M 118 78 C 112 66 124 62 130 76 L 124 92 Z", fill: "#F47C6B", stroke: "none" },
      earInnerR: { tag: "path", d: "M 202 78 C 208 66 196 62 190 76 L 196 92 Z", fill: "#F47C6B", stroke: "none" },
      // eyes normal
      eyeL: { tag: "ellipse", cx: "138", cy: "122", rx: "16", ry: "19", fill: "#FFFFFF", stroke: "#7A3B00", strokeWidth: "2.6" },
      eyeR: { tag: "ellipse", cx: "182", cy: "122", rx: "16", ry: "19", fill: "#FFFFFF", stroke: "#7A3B00", strokeWidth: "2.6" },
      pupilL: { tag: "circle", cx: "138", cy: "126", r: "8", fill: "#183B4A", stroke: "none" },
      pupilR: { tag: "circle", cx: "182", cy: "126", r: "8", fill: "#183B4A", stroke: "none" },
      highlightL: { tag: "circle", cx: "135", cy: "120", r: "3.5", fill: "#FFFFFF", stroke: "none" },
      highlightR: { tag: "circle", cx: "179", cy: "120", r: "3.5", fill: "#FFFFFF", stroke: "none" },
      nose: { tag: "path", d: "M 156 138 L 164 138 L 160 144 Z", fill: "#183B4A", stroke: "none" },
      mouth: { tag: "path", d: "M 146 150 C 152 158 168 158 174 150", fill: "none", stroke: "#7A3B00", strokeWidth: "2.8", strokeLinecap: "round" },
      // badge slot hint (subtle)
      badgeRing: { tag: "circle", cx: "160", cy: "214", r: "18", fill: "none", stroke: "#7A3B0030", strokeWidth: "2", strokeDasharray: "6 5" },
    },
    idle: { kind: "compound", parts: [
      { kind: "breathe-y", duration: 2.2, amplitude: 0.018 },
      { kind: "sway", duration: 3.4, amplitude: 0.9 },
    ]},
  },

  "anticipate": {
    paths: {
      shadow: { tag: "ellipse", cx: "160", cy: "278", rx: "52", ry: "10", fill: "#0d0d0f14", stroke: "none" },
      tail: { tag: "ellipse", cx: "102", cy: "218", rx: "18", ry: "26", fill: "#F5B82E", stroke: "#7A3B00", strokeWidth: "3" },
      tailTip: { tag: "ellipse", cx: "90", cy: "208", rx: "10", ry: "9", fill: "#FFFEF8", stroke: "none" },
      body: { tag: "ellipse", cx: "160", cy: "208", rx: "58", ry: "60", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3.2" },
      belly: { tag: "ellipse", cx: "160", cy: "226", rx: "36", ry: "28", fill: "#FFFEF8", stroke: "none" },
      armL: { tag: "path", d: "M 108 186 C 92 200 88 226 106 240 C 116 246 126 240 124 224 L 122 192 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinecap: "round", strokeLinejoin: "round" },
      armR: { tag: "path", d: "M 212 186 C 228 200 232 226 214 240 C 204 246 194 240 196 224 L 198 192 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinecap: "round", strokeLinejoin: "round" },
      head: { tag: "ellipse", cx: "160", cy: "120", rx: "62", ry: "56", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3.2" },
      cheekL: { tag: "ellipse", cx: "128", cy: "138", rx: "16", ry: "11", fill: "#F47C6B", stroke: "none", opacity: "0.22" },
      cheekR: { tag: "ellipse", cx: "192", cy: "138", rx: "16", ry: "11", fill: "#F47C6B", stroke: "none", opacity: "0.22" },
      earL: { tag: "path", d: "M 112 84 C 98 58 118 48 134 70 L 124 98 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinejoin: "round" },
      earR: { tag: "path", d: "M 208 84 C 222 58 202 48 186 70 L 196 98 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinejoin: "round" },
      earInnerL: { tag: "path", d: "M 118 74 C 112 62 124 58 130 72 L 124 88 Z", fill: "#F47C6B", stroke: "none" },
      earInnerR: { tag: "path", d: "M 202 74 C 208 62 196 58 190 72 L 196 88 Z", fill: "#F47C6B", stroke: "none" },
      eyeL: { tag: "ellipse", cx: "138", cy: "118", rx: "17", ry: "21", fill: "#FFFFFF", stroke: "#7A3B00", strokeWidth: "2.6" },
      eyeR: { tag: "ellipse", cx: "182", cy: "118", rx: "17", ry: "21", fill: "#FFFFFF", stroke: "#7A3B00", strokeWidth: "2.6" },
      pupilL: { tag: "circle", cx: "138", cy: "122", r: "8.5", fill: "#183B4A", stroke: "none" },
      pupilR: { tag: "circle", cx: "182", cy: "122", r: "8.5", fill: "#183B4A", stroke: "none" },
      highlightL: { tag: "circle", cx: "135", cy: "116", r: "3.5", fill: "#FFFFFF", stroke: "none" },
      highlightR: { tag: "circle", cx: "179", cy: "116", r: "3.5", fill: "#FFFFFF", stroke: "none" },
      nose: { tag: "path", d: "M 156 134 L 164 134 L 160 140 Z", fill: "#183B4A", stroke: "none" },
      mouth: { tag: "path", d: "M 150 148 C 156 154 164 154 170 148", fill: "none", stroke: "#7A3B00", strokeWidth: "2.8", strokeLinecap: "round" },
      badgeRing: { tag: "circle", cx: "160", cy: "212", r: "18", fill: "none", stroke: "#7A3B0030", strokeWidth: "2", strokeDasharray: "6 5" },
    },
    idle: { kind: "compound", parts: [
      { kind: "breathe-y", duration: 1.1, amplitude: 0.012 },
      { kind: "bob", duration: 0.9, amplitude: 2 },
    ]},
  },

  "celebrate": {
    paths: {
      shadow: { tag: "ellipse", cx: "160", cy: "278", rx: "58", ry: "12", fill: "#0d0d0f18", stroke: "none" },
      tail: { tag: "ellipse", cx: "106", cy: "214", rx: "20", ry: "28", fill: "#F5B82E", stroke: "#7A3B00", strokeWidth: "3" },
      tailTip: { tag: "ellipse", cx: "94", cy: "204", rx: "12", ry: "10", fill: "#FFFEF8", stroke: "none" },
      body: { tag: "ellipse", cx: "160", cy: "200", rx: "58", ry: "60", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3.2" },
      belly: { tag: "ellipse", cx: "160", cy: "218", rx: "36", ry: "28", fill: "#FFFEF8", stroke: "none" },
      // arms UP in cheer
      armL: { tag: "path", d: "M 118 148 C 92 132 86 108 108 92 C 120 84 132 92 130 110 L 138 158 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinecap: "round", strokeLinejoin: "round" },
      armR: { tag: "path", d: "M 202 148 C 228 132 234 108 212 92 C 200 84 188 92 190 110 L 182 158 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinecap: "round", strokeLinejoin: "round" },
      head: { tag: "ellipse", cx: "160", cy: "116", rx: "62", ry: "56", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3.2" },
      cheekL: { tag: "ellipse", cx: "126", cy: "138", rx: "18", ry: "12", fill: "#F47C6B", stroke: "none", opacity: "0.28" },
      cheekR: { tag: "ellipse", cx: "194", cy: "138", rx: "18", ry: "12", fill: "#F47C6B", stroke: "none", opacity: "0.28" },
      earL: { tag: "path", d: "M 112 80 C 96 54 118 44 136 66 L 124 94 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinejoin: "round" },
      earR: { tag: "path", d: "M 208 80 C 224 54 202 44 184 66 L 196 94 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinejoin: "round" },
      earInnerL: { tag: "path", d: "M 118 70 C 110 58 124 54 132 68 L 124 84 Z", fill: "#F47C6B", stroke: "none" },
      earInnerR: { tag: "path", d: "M 202 70 C 210 58 196 54 188 68 L 196 84 Z", fill: "#F47C6B", stroke: "none" },
      // star-eyes — slightly squished with joy
      eyeL: { tag: "ellipse", cx: "138", cy: "118", rx: "18", ry: "18", fill: "#FFFFFF", stroke: "#7A3B00", strokeWidth: "2.6" },
      eyeR: { tag: "ellipse", cx: "182", cy: "118", rx: "18", ry: "18", fill: "#FFFFFF", stroke: "#7A3B00", strokeWidth: "2.6" },
      pupilL: { tag: "circle", cx: "138", cy: "120", r: "9", fill: "#183B4A", stroke: "none" },
      pupilR: { tag: "circle", cx: "182", cy: "120", r: "9", fill: "#183B4A", stroke: "none" },
      highlightL: { tag: "circle", cx: "134", cy: "114", r: "4", fill: "#FFFFFF", stroke: "none" },
      highlightR: { tag: "circle", cx: "178", cy: "114", r: "4", fill: "#FFFFFF", stroke: "none" },
      nose: { tag: "path", d: "M 156 134 L 164 134 L 160 140 Z", fill: "#183B4A", stroke: "none" },
      mouth: { tag: "path", d: "M 142 148 C 150 162 170 162 178 148", fill: "none", stroke: "#7A3B00", strokeWidth: "2.8", strokeLinecap: "round" },
      badgeRing: { tag: "circle", cx: "160", cy: "206", r: "22", fill: "#087F8322", stroke: "#087F83", strokeWidth: "2.2" },
      // sparkle orphans — only in celebrate
      spark1: { tag: "path", d: "M 66 62 L 70 74 L 82 78 L 70 82 L 66 94 L 62 82 L 50 78 L 62 74 Z", fill: "#F5B82E", stroke: "#7A3B00", strokeWidth: "1.2", _orphan: true },
      spark2: { tag: "path", d: "M 248 68 L 251 76 L 259 79 L 251 82 L 248 90 L 245 82 L 237 79 L 245 76 Z", fill: "#F5B82E", stroke: "#7A3B00", strokeWidth: "1.2", _orphan: true },
      spark3: { tag: "path", d: "M 74 92 L 76 98 L 82 100 L 76 102 L 74 108 L 72 102 L 66 100 L 72 98 Z", fill: "#3C9DDB", stroke: "#183B4A", strokeWidth: "1", _orphan: true },
      spark4: { tag: "path", d: "M 244 104 L 246 110 L 252 112 L 246 114 L 244 120 L 242 114 L 236 112 L 242 110 Z", fill: "#3C9DDB", stroke: "#183B4A", strokeWidth: "1", _orphan: true },
      spark5: { tag: "path", d: "M 160 42 L 162 50 L 170 52 L 162 54 L 160 62 L 158 54 L 150 52 L 158 50 Z", fill: "#F47C6B", stroke: "#7A3B00", strokeWidth: "1", _orphan: true },
      confettiA: { tag: "path", d: "M 52 138 L 60 136 L 62 144 L 54 146 Z", fill: "#7653B5", stroke: "none", opacity: "0.95", _orphan: true },
      confettiB: { tag: "path", d: "M 268 142 L 274 138 L 276 146 L 268 148 Z", fill: "#66A83E", stroke: "none", opacity: "0.9", _orphan: true },
      confettiC: { tag: "path", d: "M 48 168 L 56 166 L 58 174 L 50 176 Z", fill: "#F47C6B", stroke: "none", opacity: "0.9", _orphan: true },
      confettiD: { tag: "path", d: "M 264 168 L 272 166 L 274 174 L 266 176 Z", fill: "#F5B82E", stroke: "none", opacity: "0.95", _orphan: true },
    },
    idle: { kind: "compound", parts: [
      { kind: "sway", duration: 2.0, amplitude: 1.2 },
      { kind: "breathe-y", duration: 1.6, amplitude: 0.015 },
      { kind: "twinkle", duration: 1.2 },
    ]},
  },

  "calm": {
    paths: {
      shadow: { tag: "ellipse", cx: "160", cy: "278", rx: "54", ry: "11", fill: "#0d0d0f14", stroke: "none" },
      tail: { tag: "ellipse", cx: "104", cy: "220", rx: "18", ry: "26", fill: "#F5B82E", stroke: "#7A3B00", strokeWidth: "3" },
      tailTip: { tag: "ellipse", cx: "92", cy: "210", rx: "10", ry: "9", fill: "#FFFEF8", stroke: "none" },
      body: { tag: "ellipse", cx: "160", cy: "210", rx: "58", ry: "60", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3.2" },
      belly: { tag: "ellipse", cx: "160", cy: "228", rx: "36", ry: "28", fill: "#FFFEF8", stroke: "none" },
      armL: { tag: "path", d: "M 112 192 C 96 206 94 232 112 246 C 122 252 132 246 130 230 L 124 198 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinecap: "round", strokeLinejoin: "round" },
      armR: { tag: "path", d: "M 208 192 C 224 206 226 232 208 246 C 198 252 188 246 190 230 L 196 198 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinecap: "round", strokeLinejoin: "round" },
      head: { tag: "ellipse", cx: "160", cy: "124", rx: "62", ry: "56", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3.2" },
      cheekL: { tag: "ellipse", cx: "128", cy: "142", rx: "15", ry: "10", fill: "#F47C6B", stroke: "none", opacity: "0.14" },
      cheekR: { tag: "ellipse", cx: "192", cy: "142", rx: "15", ry: "10", fill: "#F47C6B", stroke: "none", opacity: "0.14" },
      earL: { tag: "path", d: "M 112 88 C 98 62 118 52 134 74 L 124 102 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinejoin: "round" },
      earR: { tag: "path", d: "M 208 88 C 222 62 202 52 186 74 L 196 102 Z", fill: "#FFD76E", stroke: "#7A3B00", strokeWidth: "3", strokeLinejoin: "round" },
      earInnerL: { tag: "path", d: "M 118 78 C 112 66 124 62 130 76 L 124 92 Z", fill: "#F47C6B", stroke: "none" },
      earInnerR: { tag: "path", d: "M 202 78 C 208 66 196 62 190 76 L 196 92 Z", fill: "#F47C6B", stroke: "none" },
      eyeL: { tag: "ellipse", cx: "138", cy: "124", rx: "15", ry: "16", fill: "#FFFFFF", stroke: "#7A3B00", strokeWidth: "2.6" },
      eyeR: { tag: "ellipse", cx: "182", cy: "124", rx: "15", ry: "16", fill: "#FFFFFF", stroke: "#7A3B00", strokeWidth: "2.6" },
      pupilL: { tag: "circle", cx: "138", cy: "127", r: "7.5", fill: "#183B4A", stroke: "none" },
      pupilR: { tag: "circle", cx: "182", cy: "127", r: "7.5", fill: "#183B4A", stroke: "none" },
      highlightL: { tag: "circle", cx: "135", cy: "120", r: "3", fill: "#FFFFFF", stroke: "none" },
      highlightR: { tag: "circle", cx: "179", cy: "120", r: "3", fill: "#FFFFFF", stroke: "none" },
      nose: { tag: "path", d: "M 156 138 L 164 138 L 160 144 Z", fill: "#183B4A", stroke: "none" },
      mouth: { tag: "path", d: "M 148 150 C 154 156 166 156 172 150", fill: "none", stroke: "#7A3B00", strokeWidth: "2.6", strokeLinecap: "round" },
      badgeRing: { tag: "circle", cx: "160", cy: "214", r: "18", fill: "none", stroke: "#7A3B0030", strokeWidth: "2", strokeDasharray: "6 5" },
    },
    idle: { kind: "compound", parts: [
      { kind: "breathe-y", duration: 2.8, amplitude: 0.014 },
    ]},
  },
};
