import os
import glob
import subprocess

IMAGES_DIR = "images"
MASKS_DIR = "masks"

def main():
    os.makedirs(MASKS_DIR, exist_ok=True)

    image_paths = sorted(glob.glob(os.path.join(IMAGES_DIR, "*.png")))
    if not image_paths:
        print(f"No PNG images found in .\\{IMAGES_DIR}")
        return

    for img_path in image_paths:
        base = os.path.basename(img_path)
        mask_name = base.replace("img_", "mask_", 1) if base.startswith("img_") else f"mask_{base}"
        out_path = os.path.join(MASKS_DIR, mask_name)

        cmd = [
            "python", "png_to_mask.py",
            "--in", img_path,
            "--out", out_path,
            "--cleanup",
            "--auto_color",
            "--b_dom", "20",
            "--b_min", "30",
            "--min_area", "150",
            "--min_length", "25",
            "--fill_holes",
            "--max_hole_area", "500"  # Only fill small holes to avoid false fills
        ]

        print(f"Masking: {base} -> {mask_name}")
        subprocess.check_call(cmd)

    print("Done. Masks saved in .\\masks")

if __name__ == "__main__":
    main()
