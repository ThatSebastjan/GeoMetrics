import os
import sys
import argparse
import numpy as np
from PIL import Image
import torch
import torchvision.transforms as T
import torchvision.transforms.functional as TF
import cv2

from model import UNetSmall


class WaterDetector:
    def __init__(self, model_path, device=None, image_size=256, threshold=0.5):
        self.device = device or torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.image_size = image_size
        self.threshold = threshold

        self.model = UNetSmall(in_channels=3, out_channels=1)
        
        checkpoint = torch.load(model_path, map_location=self.device, weights_only=False)
        self.model.load_state_dict(checkpoint['model_state_dict'])
        self.model = self.model.to(self.device)
        self.model.eval()
        
        print(f"Model loaded (Trained IoU: {checkpoint.get('best_val_iou', 'N/A'):.4f})")
        print(f"Using device: {self.device}")
        print(f"Detection threshold: {self.threshold}")

        #ImageNet statistics for normalization - apparently happens to work well...
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

        #print("mask_bool:")
        #print(mask_bool)

        if (True not in mask_bool):
            #print("Early return")
            return overlay #Fix crash when there is no water

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

    parser.add_argument("--model", type=str, default="checkpoints/best_model.pth",
                        help="Path to trained model")
    
    parser.add_argument("--threshold", type=float, default=0.5,
                        help="Detection threshold (0.0-1.0)")
    
    parser.add_argument("--output", type=str, default="results",
                        help="Output directory for results")
    
    parser.add_argument("--mask_only", type=int, default=0)
    
    
    args = parser.parse_args()

    if not args.image:
        print("Error: Please provide --image")
        sys.exit(1)
    

    if not os.path.exists(args.model):
        print(f"Error: Model not found at {args.model}")
        print("Please train the model first: python train_model.py")
        sys.exit(1)

    if not os.path.exists(args.output):
        os.makedirs(args.output)
        print(f"Created results directory: {args.output}")
    

    mask_only = args.mask_only == 1

    
    detector = WaterDetector(
        model_path=args.model,
        threshold=args.threshold
    )


    img_path = args.image

    if not os.path.isabs(img_path):
        img_path = os.path.abspath(img_path)
    

    if not os.path.exists(img_path):
        print(f"Error: Image file not found: {args.image}")
        sys.exit(1)
    
    

    filename = os.path.basename(img_path)
    

    result = detector.detect(img_path)
    #status = "Water present" if result['found'] else "No water detected"
    
    base_name = os.path.splitext(filename)[0]
    mask_path = os.path.join(args.output, f"{base_name}_mask.png")
    overlay_path = os.path.join(args.output, f"{base_name}_overlay.png")
    comparison_path = os.path.join(args.output, f"{base_name}_comparison.png")
    
    try:
        cv2.imwrite(mask_path, result['mask'])

        if not mask_only:
            overlay_bgr = cv2.cvtColor(result['overlay'], cv2.COLOR_RGB2BGR)
            cv2.imwrite(overlay_path, overlay_bgr)
            comparison = create_comparison(result['original'], result['mask'], result['overlay'])
            cv2.imwrite(comparison_path, cv2.cvtColor(comparison, cv2.COLOR_RGB2BGR))

    except Exception as save_error:
        raise IOError(f"Failed to save output files: {str(save_error)}")
        
    
    print(f"\nResults saved to: {os.path.abspath(args.output)}")


def create_comparison(original, mask, overlay):
    #h, w = original.shape[:2]
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
