import os
import sys
import argparse
import glob
import numpy as np
from PIL import Image
import torch
import torchvision.transforms as T
import torchvision.transforms.functional as TF
import cv2
from tqdm import tqdm

from model import UNetSmall


class WaterDetector:
    def __init__(self, model_path, device=None, image_size=256, threshold=0.5):
        self.device = device or torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.image_size = image_size
        self.threshold = threshold
        print(f"Loading model from {model_path}...")
        self.model = UNetSmall(in_channels=3, out_channels=1)
        
        checkpoint = torch.load(model_path, map_location=self.device, weights_only=False)
        self.model.load_state_dict(checkpoint['model_state_dict'])
        self.model = self.model.to(self.device)
        self.model.eval()
        
        print(f"Model loaded! (Trained IoU: {checkpoint.get('best_val_iou', 'N/A'):.4f})")
        print(f"Using device: {self.device}")
        print(f"Detection threshold: {self.threshold}")
        self.normalize = T.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225]
        )

    def preprocess(self, image_path):
        image = Image.open(image_path).convert("RGB")
        original_size = image.size
        original_image = np.array(image)
        
        image_resized = image.resize((self.image_size, self.image_size), Image.BILINEAR)
        tensor = TF.to_tensor(image_resized)
        tensor = self.normalize(tensor)
        tensor = tensor.unsqueeze(0)
        
        return tensor, original_image, original_size

    def detect(self, image_path):
        tensor, original_image, original_size = self.preprocess(image_path)
        tensor = tensor.to(self.device)
        with torch.no_grad():
            output = self.model(tensor)
            prob = torch.sigmoid(output)
        prob = prob.squeeze().cpu().numpy()
        
        if self.device.type == "cuda":
            torch.cuda.empty_cache()
        
        mask_resized = cv2.resize(prob, original_size, interpolation=cv2.INTER_LINEAR)
        binary_mask = (mask_resized > self.threshold).astype(np.uint8) * 255
        water_pixels = np.sum(binary_mask > 0)
        total_pixels = binary_mask.shape[0] * binary_mask.shape[1]
        water_percentage = (water_pixels / total_pixels) * 100
        found = water_percentage > 0.1
        overlay = self._create_overlay(original_image, binary_mask)
        
        return {
            'found': found,
            'water_percentage': water_percentage,
            'mask': binary_mask,
            'overlay': overlay,
            'original': original_image,
            'probability_map': mask_resized
        }

    def _create_overlay(self, image, mask, color=(0, 120, 255), alpha=0.5):
        overlay = image.copy()
        water_overlay = np.zeros_like(image)
        water_overlay[mask > 0] = color
        mask_bool = mask > 0
        overlay[mask_bool] = cv2.addWeighted(
            overlay[mask_bool], 1 - alpha,
            water_overlay[mask_bool], alpha,
            0
        )
        contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        cv2.drawContours(overlay, contours, -1, (0, 255, 255), 2)
        
        return overlay


def main():
    parser = argparse.ArgumentParser(description="Detect water bodies in map images")
    parser.add_argument("--image", type=str, help="Path to a single image")
    parser.add_argument("--folder", type=str, help="Path to folder with images")
    parser.add_argument("--model", type=str, default="checkpoints/best_model.pth",
                        help="Path to trained model")
    parser.add_argument("--threshold", type=float, default=0.5,
                        help="Detection threshold (0.0-1.0)")
    parser.add_argument("--output", type=str, default="results",
                        help="Output directory for results")
    args = parser.parse_args()
    if not args.image and not args.folder:
        print("Error: Please provide --image or --folder")
        sys.exit(1)
    
    if not os.path.exists(args.model):
        print(f"Error: Model not found at {args.model}")
        print("Please train the model first: python train_model.py")
        sys.exit(1)
    if not os.path.exists(args.output):
        os.makedirs(args.output)
        print(f"Created results directory: {args.output}")
    detector = WaterDetector(
        model_path=args.model,
        threshold=args.threshold
    )
    if args.image:
        img_path = args.image
        if not os.path.isabs(img_path):
            img_path = os.path.abspath(img_path)
        
        if not os.path.exists(img_path):
            possible_paths = [
                img_path,
                os.path.join("test_images", os.path.basename(img_path)),
                os.path.join("images", os.path.basename(img_path)),
            ]
            found = False
            for path in possible_paths:
                if os.path.exists(path):
                    img_path = os.path.abspath(path)
                    found = True
                    break
            
            if not found:
                print(f"Error: Image file not found: {args.image}")
                print(f"Tried: {possible_paths}")
                print(f"Current working directory: {os.getcwd()}")
                sys.exit(1)
        
        image_paths = [img_path]
    else:
        if not os.path.exists(args.folder):
            print(f"Error: Folder not found: {args.folder}")
            sys.exit(1)
        image_paths = glob.glob(os.path.join(args.folder, "*.png"))
        image_paths += glob.glob(os.path.join(args.folder, "*.jpg"))
        image_paths += glob.glob(os.path.join(args.folder, "*.jpeg"))
        image_paths = sorted(image_paths)
    
    if not image_paths:
        print("No images found!")
        sys.exit(1)
    
    print(f"\nProcessing {len(image_paths)} image(s)...")
    print("="*60)
    
    results_summary = []
    
    for img_path in tqdm(image_paths, desc="Processing images"):
        filename = os.path.basename(img_path)
        
        try:
            if not os.path.exists(img_path):
                raise FileNotFoundError(f"Image file not found: {img_path}")
            
            result = detector.detect(img_path)
            status = "WATER FOUND" if result['found'] else "No water detected"
            
            base_name = os.path.splitext(filename)[0]
            mask_path = os.path.join(args.output, f"{base_name}_mask.png")
            overlay_path = os.path.join(args.output, f"{base_name}_overlay.png")
            comparison_path = os.path.join(args.output, f"{base_name}_comparison.png")
            
            try:
                cv2.imwrite(mask_path, result['mask'])
                overlay_bgr = cv2.cvtColor(result['overlay'], cv2.COLOR_RGB2BGR)
                cv2.imwrite(overlay_path, overlay_bgr)
                comparison = create_comparison(result['original'], result['mask'], result['overlay'])
                cv2.imwrite(comparison_path, cv2.cvtColor(comparison, cv2.COLOR_RGB2BGR))
            except Exception as save_error:
                raise IOError(f"Failed to save output files: {str(save_error)}")
            
            results_summary.append({
                'file': filename,
                'found': result['found'],
                'percentage': result['water_percentage']
            })
            
            tqdm.write(f"{filename}: {status} ({result['water_percentage']:.2f}% water)")
            
        except Exception as e:
            error_msg = str(e)
            tqdm.write(f"{filename}: ERROR - {error_msg}")
            results_summary.append({
                'file': filename,
                'found': False,
                'percentage': 0,
                'error': error_msg
            })
    print("\n" + "="*60)
    print("SUMMARY")
    print("="*60)
    
    found_count = sum(1 for r in results_summary if r['found'])
    total_count = len(results_summary)
    
    print(f"Total images processed: {total_count}")
    print(f"Images with water detected: {found_count}")
    print(f"Images without water: {total_count - found_count}")
    print(f"\nResults saved to: {os.path.abspath(args.output)}")


def create_comparison(original, mask, overlay):
    h, w = original.shape[:2]
    mask_rgb = cv2.cvtColor(mask, cv2.COLOR_GRAY2RGB)
    font = cv2.FONT_HERSHEY_SIMPLEX
    original_labeled = original.copy()
    mask_labeled = mask_rgb.copy()
    overlay_labeled = overlay.copy()
    cv2.putText(original_labeled, "Original", (10, 30), font, 0.8, (255, 255, 255), 2)
    cv2.putText(mask_labeled, "Detected Mask", (10, 30), font, 0.8, (255, 255, 255), 2)
    cv2.putText(overlay_labeled, "Overlay", (10, 30), font, 0.8, (255, 255, 255), 2)
    comparison = np.hstack([original_labeled, mask_labeled, overlay_labeled])
    
    return comparison


if __name__ == "__main__":
    main()
