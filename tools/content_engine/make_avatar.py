from PIL import Image, ImageDraw
from pathlib import Path

source_path = Path("/home/ron/projects/maxines-world/.hermes/desktop-attachments/android-icon/maxines-world-android-icon-v1/source/maxines_world_icon_master_1024.png")
output_path = Path("/home/ron/workspace/maxines-world/tools/content_engine/milo_icon_anchor.png")

img = Image.open(source_path).convert("RGBA")
# Create circular mask
mask = Image.new("L", img.size, 0)
draw = ImageDraw.Draw(mask)
draw.ellipse((0, 0, img.size[0], img.size[1]), fill=255)

circular_img = Image.new("RGBA", img.size, (0, 0, 0, 0))
circular_img.paste(img, (0, 0), mask=mask)

avatar = circular_img.resize((256, 256), Image.Resampling.LANCZOS)
avatar.save(output_path, "PNG")
print(f"Generated exact icon anchor at: {output_path} ({output_path.stat().st_size} bytes)")
