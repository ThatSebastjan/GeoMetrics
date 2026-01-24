import os
import glob
import numpy as np
from PIL import Image
import torch
from torch.utils.data import Dataset
import torchvision.transforms as T
import torchvision.transforms.functional as TF
import random
import warnings


class WaterDataset(Dataset):
    def __init__(self, images_dir, masks_dir, image_size=256, augment=True, validate_data=True):
        self.images_dir = images_dir
        self.masks_dir = masks_dir
        self.image_size = image_size
        self.augment = augment
        self.image_files = sorted(glob.glob(os.path.join(images_dir, "*.png")))

        if len(self.image_files) == 0:
            raise ValueError(f"No images found in {images_dir}")
        
        if validate_data:
            self.image_files = self._validate_data(self.image_files, masks_dir)
        
        print(f"Found {len(self.image_files)} valid image-mask pairs")

        self.normalize = T.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225]
        )
    

    #Check image-mask pairs to match...
    def _validate_data(self, image_files, masks_dir):
        valid_files = []
        skipped_count = 0
        
        for img_path in image_files:
            try:
                with Image.open(img_path) as img:
                    img.verify()
                
                img = Image.open(img_path)
                img_width, img_height = img.size
                img.close()
                
                base = os.path.basename(img_path)
                mask_name = base.replace("img_", "mask_", 1) if base.startswith("img_") else f"mask_{base}"
                mask_path = os.path.join(masks_dir, mask_name)
                
                if not os.path.exists(mask_path):
                    warnings.warn(f"Skipping {base}: mask not found ({mask_name})")
                    skipped_count += 1
                    continue
                
                with Image.open(mask_path) as mask:
                    mask.verify()
                
                mask = Image.open(mask_path)
                mask_width, mask_height = mask.size
                mask.close()
                
                if (img_width, img_height) != (mask_width, mask_height):
                    warnings.warn(
                        f"Skipping {base}: dimension mismatch - "
                        f"image {img_width}x{img_height}, mask {mask_width}x{mask_height}"
                    )
                    skipped_count += 1
                    continue
                
                img = Image.open(img_path)
                if img.mode not in ("RGB", "P", "RGBA", "LA"):
                    warnings.warn(f"Skipping {base}: image mode not supported (mode: {img.mode})")
                    img.close()
                    skipped_count += 1
                    continue
                img.close()
                
                mask = Image.open(mask_path)
                if mask.mode not in ("L", "1"):
                    warnings.warn(f"Skipping {base}: mask is not grayscale (mode: {mask.mode})")
                    mask.close()
                    skipped_count += 1
                    continue
                mask.close()
                
                valid_files.append(img_path)
                
            except Exception as e:
                warnings.warn(f"Skipping {os.path.basename(img_path)}: {str(e)}")
                skipped_count += 1
                continue
        
        if skipped_count > 0:
            print(f"Warning: Skipped {skipped_count} invalid image-mask pairs")
        
        if len(valid_files) == 0:
            raise ValueError("No valid image and mask pair exists...")
        
        return valid_files


    def __len__(self):
        return len(self.image_files)


    def __getitem__(self, idx):
        img_path = self.image_files[idx]
        
        try:
            image = Image.open(img_path).convert("RGB")
        except Exception as e:
            raise ValueError(f"Failed to load image {img_path}: {str(e)}")
        
        base = os.path.basename(img_path)
        mask_name = base.replace("img_", "mask_", 1) if base.startswith("img_") else f"mask_{base}"
        mask_path = os.path.join(self.masks_dir, mask_name)
        
        if not os.path.exists(mask_path):
            raise FileNotFoundError(f"Mask not found: {mask_path}")
        
        try:
            mask = Image.open(mask_path).convert("L")
        except Exception as e:
            raise ValueError(f"Failed to load mask {mask_path}: {str(e)}")
        
        image = image.resize((self.image_size, self.image_size), Image.BILINEAR)
        mask = mask.resize((self.image_size, self.image_size), Image.NEAREST)
        
        #Augmentation?
        if self.augment:
            image, mask = self._augment(image, mask)

            image = TF.to_tensor(image)
            image = self.normalize(image)
            mask = np.array(mask, dtype=np.float32)
            mask = mask / 255.0
            mask = torch.from_numpy(mask).unsqueeze(0)

        return image, mask


    def _augment(self, image, mask):
        if random.random() > 0.5:
            image = TF.hflip(image)
            mask = TF.hflip(mask)

        if random.random() > 0.5:
            image = TF.vflip(image)
            mask = TF.vflip(mask)

        if random.random() > 0.5:
            angle = random.choice([90, 180, 270])
            image = TF.rotate(image, angle)
            mask = TF.rotate(mask, angle)

        if random.random() > 0.5:
            image = TF.adjust_brightness(image, random.uniform(0.8, 1.2))

        if random.random() > 0.5:
            image = TF.adjust_contrast(image, random.uniform(0.8, 1.2))

        if random.random() > 0.5:
            image = TF.adjust_saturation(image, random.uniform(0.8, 1.2))

        return image, mask
