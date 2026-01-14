import os
import glob
import numpy as np
from PIL import Image
import torch
from torch.utils.data import Dataset
import torchvision.transforms as T
import torchvision.transforms.functional as TF
import random


class WaterDataset(Dataset):
    def __init__(self, images_dir, masks_dir, image_size=256, augment=True):
        self.images_dir = images_dir
        self.masks_dir = masks_dir
        self.image_size = image_size
        self.augment = augment
        self.image_files = sorted(glob.glob(os.path.join(images_dir, "*.png")))
        if len(self.image_files) == 0:
            raise ValueError(f"No images found in {images_dir}")
        print(f"Found {len(self.image_files)} images")
        self.normalize = T.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225]
        )

    def __len__(self):
        return len(self.image_files)

    def __getitem__(self, idx):
        img_path = self.image_files[idx]
        image = Image.open(img_path).convert("RGB")
        base = os.path.basename(img_path)
        mask_name = base.replace("img_", "mask_", 1) if base.startswith("img_") else f"mask_{base}"
        mask_path = os.path.join(self.masks_dir, mask_name)
        if not os.path.exists(mask_path):
            raise FileNotFoundError(f"Mask not found: {mask_path}")
        mask = Image.open(mask_path).convert("L")
        image = image.resize((self.image_size, self.image_size), Image.BILINEAR)
        mask = mask.resize((self.image_size, self.image_size), Image.NEAREST)
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


class WaterDatasetInference(Dataset):
    def __init__(self, image_paths, image_size=256):
        self.image_paths = image_paths if isinstance(image_paths, list) else [image_paths]
        self.image_size = image_size
        self.normalize = T.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225]
        )

    def __len__(self):
        return len(self.image_paths)

    def __getitem__(self, idx):
        img_path = self.image_paths[idx]
        image = Image.open(img_path).convert("RGB")
        original_size = image.size
        image = image.resize((self.image_size, self.image_size), Image.BILINEAR)
        image = TF.to_tensor(image)
        image = self.normalize(image)
        return image, img_path, original_size


if __name__ == "__main__":
    dataset = WaterDataset("images", "masks", image_size=256, augment=False)
    print(f"Dataset size: {len(dataset)}")
    img, mask = dataset[0]
    print(f"Image shape: {img.shape}, dtype: {img.dtype}")
    print(f"Mask shape: {mask.shape}, dtype: {mask.dtype}")
    print(f"Mask unique values: {torch.unique(mask)}")
