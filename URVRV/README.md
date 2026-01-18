# Water Body Detection on Terrain Maps

AI model that detects water bodies (lakes, rivers, streams) on OpenStreetMap-style terrain maps using deep learning.

## Requirements

### Python Version
**Python 3.11** is required (PyTorch does not yet support Python 3.12+)

Download Python 3.11 from: https://www.python.org/downloads/release/python-3119/

During installation, make sure to check **"Add Python to PATH"**

### Hardware
- **GPU (Optional but recommended)**: NVIDIA GPU with CUDA support for faster training/inference
- **CPU only**: Works but slower

## Installation

### 1. Create Virtual Environment
```bash
# Windows
py -3.11 -m venv venv
.\venv\Scripts\activate

# Linux/Mac
python3.11 -m venv venv
source venv/bin/activate
```

### 2. Install Dependencies
```bash
# For GPU (NVIDIA CUDA 12.1+)
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu121

# For CPU only
pip install torch torchvision

# Install additional dependencies
pip install numpy Pillow opencv-python tqdm
```

## Usage

Follow these steps in order to create and use your water detection model:

### Step 1: Download Map Screenshots

Use `download_tiles.py` to automatically download map images from OpenStreetMap to create your training dataset.

**Run the script with command-line arguments** to set your area of interest:
- `--minlat`, `--minlon`, `--maxlat`, `--maxlon`: Define the bounding box (latitude/longitude coordinates)
- `--z`: Set zoom level (higher = more detail, default: 15)
- `--count`: Number of images to download (default: 200)
- `--sleep`: Delay between downloads in seconds (default: 1.0)

```bash
# Activate virtual environment first
.\venv\Scripts\activate  # Windows
# source venv/bin/activate  # Linux/Mac

# Example: Download 200 images from a specific area
# These coordinates were used to create the current images in \images folder
python download_tiles.py --z 15 --count 200 --minlat 46.6220022453 --minlon 16.1304616928 --maxlat 46.6486112860 --maxlon 16.2001991272 --sleep 1.0
```

**Note**: All coordinate arguments (`--minlat`, `--minlon`, `--maxlat`, `--maxlon`) are required.

This will create an `images/` folder with map screenshots (e.g., `img_001.png`, `img_002.png`, ...).

**Recommendation**: Download at least 100-200 images for good model performance.

---

### Step 2: Generate Masks from Images

Convert your downloaded map images into binary masks (white = water, black = land) using automatic color detection.

```bash
# Generate masks using automatic water detection
python batch_masks.py
```

This creates a `masks/` folder with corresponding masks for each image.

**After generation**: Check a few masks to ensure quality. If they're not accurate, adjust parameters in the batch script.

---

### Step 3: Train the Model

Train the neural network using your images and masks.

```bash
# Train with default settings (50 epochs, ~10-20 minutes on GPU)
python train_model.py

# Custom training parameters
python train_model.py --epochs 100 --batch_size 8 --lr 0.0001

# Resume training from checkpoint
python train_model.py --resume checkpoints/best_model.pth

# Advanced options
python train_model.py --early_stop_patience 20 --max_grad_norm 1.0 --num_workers 4
```

**Available training arguments:**
- `--images_dir`: Directory with training images (default: `images`)
- `--masks_dir`: Directory with mask images (default: `masks`)
- `--epochs`: Number of training epochs (default: 50)
- `--batch_size`: Batch size (default: 4)
- `--lr`: Learning rate (default: 0.0001)
- `--image_size`: Input image size (default: 256)
- `--val_split`: Validation split ratio (default: 0.15)
- `--early_stop_patience`: Early stopping patience in epochs (default: 15, set to 0 to disable)
- `--max_grad_norm`: Maximum gradient norm for clipping (default: 1.0, set to 0 to disable)
- `--num_workers`: DataLoader workers (default: auto-detect)
- `--save_dir`: Directory to save checkpoints (default: `checkpoints`)

**Training output:**
- Progress bars showing loss, IoU, and Dice scores
- `checkpoints/best_model.pth` - Best performing model
- `checkpoints/checkpoint_epoch_*.pth` - Periodic checkpoints

---

### Step 4: Detect Water Bodies in New Images

Use your trained model to detect water in new map screenshots.

```bash
# Detect water in a single image
python detect_water.py --image path/to/your/map.png

# Process multiple images in a folder
python detect_water.py --folder path/to/images/

# Adjust detection sensitivity (0.0-1.0, default: 0.5)
python detect_water.py --image map.png --threshold 0.3

# Use custom model or output directory
python detect_water.py --folder images/ --model checkpoints/best_model.pth --output my_results/

# Example usage
python detect_water.py --image test_001.png
```

**Available detection arguments:**
- `--image`: Path to a single image file
- `--folder`: Path to folder containing images
- `--model`: Path to trained model (default: `checkpoints/best_model.pth`)
- `--threshold`: Detection threshold 0.0-1.0 (default: 0.5)
- `--output`: Output directory for results (default: `results`)

**Output** (saved to output directory):
- `*_mask.png` - Binary mask (white = water, black = land)
- `*_overlay.png` - Original image with water highlighted in blue
- `*_comparison.png` - Side-by-side comparison of all three

```