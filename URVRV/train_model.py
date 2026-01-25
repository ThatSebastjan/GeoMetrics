import os
import argparse
import time
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, random_split
from tqdm import tqdm

from model import UNetSmall
from dataset import WaterDataset


class DiceLoss(nn.Module):
    def __init__(self, smooth=1.0):
        super().__init__()
        self.smooth = smooth

    def forward(self, pred, target):
        pred = torch.sigmoid(pred)
        
        pred_flat = pred.view(-1)
        target_flat = target.view(-1)
        
        intersection = (pred_flat * target_flat).sum()
        dice = (2. * intersection + self.smooth) / (pred_flat.sum() + target_flat.sum() + self.smooth)
        
        return 1 - dice


class CombinedLoss(nn.Module):
    def __init__(self, bce_weight=0.5, dice_weight=0.5):
        super().__init__()
        self.bce = nn.BCEWithLogitsLoss()
        self.dice = DiceLoss()
        self.bce_weight = bce_weight
        self.dice_weight = dice_weight

    def forward(self, pred, target):
        return self.bce_weight * self.bce(pred, target) + self.dice_weight * self.dice(pred, target)


def calculate_metrics(pred, target, threshold=0.5):
    pred = (torch.sigmoid(pred) > threshold).float()
    
    intersection = (pred * target).sum()
    union = pred.sum() + target.sum() - intersection
    
    iou = (intersection + 1e-6) / (union + 1e-6)
    dice = (2 * intersection + 1e-6) / (pred.sum() + target.sum() + 1e-6)
    
    return iou.item(), dice.item()


def train_one_epoch(model, loader, criterion, optimizer, device, max_grad_norm=None):
    model.train()
    total_loss = 0
    total_iou = 0
    total_dice = 0
    
    pbar = tqdm(loader, desc="Training")
    for images, masks in pbar:
        images = images.to(device)
        masks = masks.to(device)
        
        optimizer.zero_grad()
        
        outputs = model(images)
        loss = criterion(outputs, masks)
        
        loss.backward()
        
        if max_grad_norm is not None:
            torch.nn.utils.clip_grad_norm_(model.parameters(), max_grad_norm)
        
        optimizer.step()
        
        iou, dice = calculate_metrics(outputs, masks)
        
        total_loss += loss.item()
        total_iou += iou
        total_dice += dice
        
        pbar.set_postfix(loss=f"{loss.item():.4f}", iou=f"{iou:.4f}")
    
    n = len(loader)
    return total_loss / n, total_iou / n, total_dice / n


def validate(model, loader, criterion, device):
    model.eval()
    total_loss = 0
    total_iou = 0
    total_dice = 0
    
    with torch.no_grad():
        for images, masks in loader:
            images = images.to(device)
            masks = masks.to(device)
            
            outputs = model(images)
            loss = criterion(outputs, masks)
            
            iou, dice = calculate_metrics(outputs, masks)
            
            total_loss += loss.item()
            total_iou += iou
            total_dice += dice
    
    n = len(loader)
    return total_loss / n, total_iou / n, total_dice / n


def main():
    parser = argparse.ArgumentParser(description="Train water body detection model")
    parser.add_argument("--images_dir", type=str, default="images")
    parser.add_argument("--masks_dir", type=str, default="masks")
    parser.add_argument("--epochs", type=int, default=50)
    parser.add_argument("--batch_size", type=int, default=4)
    parser.add_argument("--lr", type=float, default=1e-4)
    parser.add_argument("--image_size", type=int, default=256)
    parser.add_argument("--val_split", type=float, default=0.15)
    parser.add_argument("--resume", type=str, default=None)
    parser.add_argument("--save_dir", type=str, default="checkpoints")
    parser.add_argument("--early_stop_patience", type=int, default=15,
                        help="Early stopping patience (epochs without improvement). Set to 0 to disable.")
    parser.add_argument("--max_grad_norm", type=float, default=1.0,
                        help="Maximum gradient norm for clipping. Set to 0 to disable.")
    parser.add_argument("--num_workers", type=int, default=None,
                        help="Number of DataLoader workers (default: auto-detect)")
    args = parser.parse_args()
    os.makedirs(args.save_dir, exist_ok=True)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Using device: {device}")
    print("Loading dataset...")
    full_dataset = WaterDataset(
        args.images_dir, 
        args.masks_dir, 
        image_size=args.image_size,
        augment=True
    )
    val_size = int(len(full_dataset) * args.val_split)
    train_size = len(full_dataset) - val_size
    train_dataset, val_dataset = random_split(
        full_dataset, 
        [train_size, val_size],
        generator=torch.Generator().manual_seed(42)
    )
    val_dataset.dataset.augment = False
    print(f"Training samples: {train_size}")
    print(f"Validation samples: {val_size}")
    
    if args.num_workers is None:
        num_workers = min(4, os.cpu_count() or 1)
    else:
        num_workers = args.num_workers
    
    train_loader = DataLoader(
        train_dataset, 
        batch_size=args.batch_size, 
        shuffle=True,
        num_workers=num_workers,
        pin_memory=True if device.type == "cuda" else False,
        persistent_workers=True if num_workers > 0 else False
    )
    val_loader = DataLoader(
        val_dataset, 
        batch_size=args.batch_size, 
        shuffle=False,
        num_workers=num_workers,
        pin_memory=True if device.type == "cuda" else False,
        persistent_workers=True if num_workers > 0 else False
    )
    model = UNetSmall(in_channels=3, out_channels=1)
    model = model.to(device)
    print(f"Model parameters: {sum(p.numel() for p in model.parameters()):,}")
    criterion = CombinedLoss(bce_weight=0.5, dice_weight=0.5)
    optimizer = optim.AdamW(model.parameters(), lr=args.lr, weight_decay=1e-4)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(
        optimizer, mode='min', factor=0.5, patience=5, verbose=True
    )
    start_epoch = 0
    best_val_iou = 0
    epochs_without_improvement = 0
    
    if args.resume and os.path.exists(args.resume):
        print(f"Resuming from {args.resume}")
        checkpoint = torch.load(args.resume, map_location=device)
        model.load_state_dict(checkpoint['model_state_dict'])
        optimizer.load_state_dict(checkpoint['optimizer_state_dict'])
        if 'scheduler_state_dict' in checkpoint:
            scheduler.load_state_dict(checkpoint['scheduler_state_dict'])
        start_epoch = checkpoint.get('epoch', 0)
        best_val_iou = checkpoint.get('best_val_iou', 0)
        epochs_without_improvement = checkpoint.get('epochs_without_improvement', 0)
        print(f"Resumed from epoch {start_epoch}, best IoU: {best_val_iou:.4f}")
    print("\n" + "="*50)
    print("Starting training...")
    print("="*50 + "\n")
    if args.early_stop_patience > 0:
        print(f"Early stopping enabled (patience: {args.early_stop_patience} epochs)")
    if args.max_grad_norm > 0:
        print(f"Gradient clipping enabled (max_norm: {args.max_grad_norm})")
    print(f"DataLoader workers: {num_workers}")
    print()
    
    for epoch in range(start_epoch, args.epochs):
        start_time = time.time()
        train_loss, train_iou, train_dice = train_one_epoch(
            model, train_loader, criterion, optimizer, device,
            max_grad_norm=args.max_grad_norm if args.max_grad_norm > 0 else None
        )
        val_loss, val_iou, val_dice = validate(model, val_loader, criterion, device)
        scheduler.step(val_loss)
        
        epoch_time = time.time() - start_time
        
        improved = val_iou > best_val_iou
        if improved:
            best_val_iou = val_iou
            epochs_without_improvement = 0
        else:
            epochs_without_improvement += 1
        
        print(f"\nEpoch {epoch+1}/{args.epochs} ({epoch_time:.1f}s)")
        print(f"  Train - Loss: {train_loss:.4f}, IoU: {train_iou:.4f}, Dice: {train_dice:.4f}")
        print(f"  Val   - Loss: {val_loss:.4f}, IoU: {val_iou:.4f}, Dice: {val_dice:.4f}")
        print(f"  LR: {optimizer.param_groups[0]['lr']:.2e}")
        
        if improved:
            torch.save({
                'epoch': epoch + 1,
                'model_state_dict': model.state_dict(),
                'optimizer_state_dict': optimizer.state_dict(),
                'scheduler_state_dict': scheduler.state_dict(),
                'best_val_iou': best_val_iou,
                'val_dice': val_dice,
                'epochs_without_improvement': epochs_without_improvement,
            }, os.path.join(args.save_dir, "best_model.pth"))
            print(f"  Saved best model (IoU: {best_val_iou:.4f})")
        else:
            print(f"  No improvement ({epochs_without_improvement}/{args.early_stop_patience if args.early_stop_patience > 0 else 'N/A'})")
        
        if (epoch + 1) % 10 == 0:
            torch.save({
                'epoch': epoch + 1,
                'model_state_dict': model.state_dict(),
                'optimizer_state_dict': optimizer.state_dict(),
                'scheduler_state_dict': scheduler.state_dict(),
                'best_val_iou': best_val_iou,
                'epochs_without_improvement': epochs_without_improvement,
            }, os.path.join(args.save_dir, f"checkpoint_epoch_{epoch+1}.pth"))
        
        if args.early_stop_patience > 0 and epochs_without_improvement >= args.early_stop_patience:
            print(f"\nEarly stopping triggered after {epochs_without_improvement} epochs without improvement")
            break
    
    print("\n" + "="*50)
    print(f"Training complete! Best validation IoU: {best_val_iou:.4f}")
    print(f"Best model saved to: {args.save_dir}/best_model.pth")
    print("="*50)


if __name__ == "__main__":
    main()
