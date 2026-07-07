import argparse
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "Pet_Design_Drawing" / "Status"
OUT_ROOT = ROOT / "web-assets" / "pet"

BACKGROUND_THRESHOLD = 238
MIN_COMPONENT_AREA = 800
ANCHOR_COMPONENT_AREA = 10_000
CANVAS_SIZE = 288
FRAME_DURATION_MS = 200
MERGE_GAP = 24

FRAME_REGIONS = {
    "idle": {"top": 90, "bottom": 720, "row_split": 400},
    "doubt": {"top": 90, "bottom": 820, "row_split": 450},
    "move": {"top": 90, "bottom": 820, "row_split": 470},
    "rest": {"top": 90, "bottom": 820, "row_split": 450, "component_min": 80, "attach": "right"},
    "sad": {"top": 90, "bottom": 820, "row_split": 450},
    "watering": {"top": 90, "bottom": 850, "row_split": 450},
    "work": {"top": 90, "bottom": 820, "row_split": 450},
}


def is_background(rgb):
    return (
        rgb[..., 0] > BACKGROUND_THRESHOLD
    ) & (
        rgb[..., 1] > BACKGROUND_THRESHOLD
    ) & (
        rgb[..., 2] > BACKGROUND_THRESHOLD
    )


def find_sprite_boxes(image, region):
    arr = np.array(image.convert("RGBA"))
    mask = ~is_background(arr[:, :, :3])
    mask[: region["top"], :] = False
    mask[region["bottom"] :, :] = False

    height, width = mask.shape
    seen = np.zeros_like(mask, dtype=bool)
    boxes = []

    for y in range(height):
        for start_x in np.where(mask[y] & ~seen[y])[0]:
            if seen[y, start_x] or not mask[y, start_x]:
                continue

            queue = deque([(int(start_x), int(y))])
            seen[y, start_x] = True
            min_x = max_x = int(start_x)
            min_y = max_y = int(y)
            area = 0

            while queue:
                x, current_y = queue.pop()
                area += 1
                min_x = min(min_x, x)
                max_x = max(max_x, x)
                min_y = min(min_y, current_y)
                max_y = max(max_y, current_y)

                for next_x, next_y in (
                    (x + 1, current_y),
                    (x - 1, current_y),
                    (x, current_y + 1),
                    (x, current_y - 1),
                ):
                    if (
                        0 <= next_x < width
                        and 0 <= next_y < height
                        and mask[next_y, next_x]
                        and not seen[next_y, next_x]
                    ):
                        seen[next_y, next_x] = True
                        queue.append((next_x, next_y))

            if area >= region.get("component_min", MIN_COMPONENT_AREA):
                boxes.append((area, (min_x, min_y, max_x + 1, max_y + 1)))

    boxes = attach_nearby_parts(boxes, region)
    top_row = merge_nearby_boxes(
        sorted([box for box in boxes if box[1] < region["row_split"]], key=lambda box: box[0])
    )
    bottom_row = merge_nearby_boxes(
        sorted([box for box in boxes if box[1] >= region["row_split"]], key=lambda box: box[0])
    )
    return top_row + bottom_row


def attach_nearby_parts(components, region):
    anchors = [box for area, box in components if area >= ANCHOR_COMPONENT_AREA]
    parts = [box for area, box in components if area < ANCHOR_COMPONENT_AREA]
    boxes = []

    for anchor in anchors:
        min_x, min_y, max_x, max_y = anchor
        for part in parts:
            center_x = (part[0] + part[2]) / 2
            center_y = (part[1] + part[3]) / 2

            if region.get("attach") == "right":
                should_attach = (
                    max_x - 8 <= center_x <= max_x + 70
                    and min_y - 30 <= center_y <= max_y + 12
                )
            else:
                should_attach = (
                    min_x - 80 <= center_x <= max_x + 80
                    and min_y - 80 <= center_y <= max_y + 80
                )

            if should_attach:
                min_x = min(min_x, part[0])
                min_y = min(min_y, part[1])
                max_x = max(max_x, part[2])
                max_y = max(max_y, part[3])
        boxes.append((min_x, min_y, max_x, max_y))

    return boxes


def merge_nearby_boxes(boxes):
    merged = []
    for box in boxes:
        if not merged:
            merged.append(box)
            continue

        previous = merged[-1]
        gap = box[0] - previous[2]
        overlaps_y = box[1] <= previous[3] and box[3] >= previous[1]
        if gap <= MERGE_GAP and overlaps_y:
            merged[-1] = (
                min(previous[0], box[0]),
                min(previous[1], box[1]),
                max(previous[2], box[2]),
                max(previous[3], box[3]),
            )
        else:
            merged.append(box)
    return merged


def remove_connected_background(frame):
    arr = np.array(frame.convert("RGBA"))
    bg = is_background(arr[:, :, :3])
    height, width = bg.shape
    remove = np.zeros_like(bg, dtype=bool)
    queue = deque()

    for x in range(width):
        if bg[0, x]:
            queue.append((x, 0))
        if bg[height - 1, x]:
            queue.append((x, height - 1))

    for y in range(height):
        if bg[y, 0]:
            queue.append((0, y))
        if bg[y, width - 1]:
            queue.append((width - 1, y))

    while queue:
        x, y = queue.pop()
        if remove[y, x] or not bg[y, x]:
            continue

        remove[y, x] = True
        for next_x, next_y in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= next_x < width and 0 <= next_y < height and not remove[next_y, next_x]:
                queue.append((next_x, next_y))

    arr[remove, 3] = 0
    return Image.fromarray(arr, "RGBA")


def make_canvas(sprite):
    bbox = sprite.getbbox()
    sprite = sprite.crop(bbox)
    canvas = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (255, 255, 255, 0))
    x = (CANVAS_SIZE - sprite.width) // 2
    y = CANVAS_SIZE - sprite.height - 18
    canvas.alpha_composite(sprite, (x, y))
    return canvas


def save_transparent_gif(frames, gif_path):
    gif_frames = []
    for frame in frames:
        frame = frame.convert("RGBA")
        alpha = frame.getchannel("A")
        gif_frame = frame.convert("P", palette=Image.Palette.ADAPTIVE, colors=255)
        transparent_pixels = alpha.point(lambda value: 255 if value == 0 else 0)
        gif_frame.paste(255, transparent_pixels)
        gif_frame.info["transparency"] = 255
        gif_frames.append(gif_frame)

    gif_frames[0].save(
        gif_path,
        save_all=True,
        append_images=gif_frames[1:],
        duration=FRAME_DURATION_MS,
        loop=0,
        disposal=2,
        transparency=255,
    )


def extract_status(state):
    if state not in FRAME_REGIONS:
        raise RuntimeError(f"No frame region configured for {state}")

    source_path = SOURCE_DIR / f"{state}.png"
    out_dir = OUT_ROOT / state
    frames_dir = out_dir / "frames"
    gif_path = out_dir / f"{state}.gif"

    out_dir.mkdir(parents=True, exist_ok=True)
    frames_dir.mkdir(parents=True, exist_ok=True)

    source = Image.open(source_path).convert("RGBA")
    boxes = find_sprite_boxes(source, FRAME_REGIONS[state])
    if len(boxes) != 8:
        raise RuntimeError(f"Expected 8 {state} frames, found {len(boxes)}: {boxes}")

    frames = []
    for index, box in enumerate(boxes, start=1):
        padded = (
            max(0, box[0] - 12),
            max(0, box[1] - 12),
            min(source.width, box[2] + 12),
            min(source.height, box[3] + 12),
        )
        frame = source.crop(padded)
        frame = remove_connected_background(frame)
        frame = make_canvas(frame)
        frame.save(frames_dir / f"{index:02d}.png")
        frames.append(frame)

    save_transparent_gif(frames, gif_path)
    print(f"Wrote {len(frames)} frames to {frames_dir}")
    print(f"Wrote GIF to {gif_path}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("state", choices=sorted(FRAME_REGIONS))
    args = parser.parse_args()
    extract_status(args.state)


if __name__ == "__main__":
    main()
