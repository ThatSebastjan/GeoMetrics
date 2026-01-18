import os
import argparse
import numpy as np
import cv2


def fill_holes_per_component(mask: np.ndarray, max_hole_area: int = 500) -> np.ndarray:
    """
    Fill holes INSIDE each connected water component, but only if
    the hole is small enough. This prevents filling large background
    areas that happen to be enclosed.
    
    Args:
        mask: Binary mask (0/255 uint8)
        max_hole_area: Maximum hole area to fill (prevents filling large gaps)
    
    Returns:
        Mask with small internal holes filled
    """
    result = mask.copy()
    
    # Find contours of water regions (external only)
    contours, hierarchy = cv2.findContours(
        mask, cv2.RETR_CCOMP, cv2.CHAIN_APPROX_SIMPLE
    )
    
    if hierarchy is None:
        return result
    
    hierarchy = hierarchy[0]
    
    for i, (cnt, h) in enumerate(zip(contours, hierarchy)):
        parent_idx = h[3]
        if parent_idx >= 0:
            hole_area = cv2.contourArea(cnt)
            if hole_area <= max_hole_area:
                cv2.drawContours(result, [cnt], -1, 255, thickness=cv2.FILLED)
    
    return result


def fill_holes_legacy(mask: np.ndarray) -> np.ndarray:
    """
    Legacy flood-fill approach. DEPRECATED: Can incorrectly fill areas
    connected to image edges. Use fill_holes_per_component instead.
    """
    h, w = mask.shape[:2]
    flood = mask.copy()
    ff = np.zeros((h + 2, w + 2), dtype=np.uint8)
    cv2.floodFill(flood, ff, (0, 0), 255)
    flood_inv = cv2.bitwise_not(flood)
    return cv2.bitwise_or(mask, flood_inv)


def filter_components(mask: np.ndarray, min_area: int, min_length: int,
                      aspect_ratio_threshold: float = 0.15) -> np.ndarray:
    """
    Keep connected components that are either:
      - large enough by area (lakes), OR
      - long enough AND elongated (rivers/streams).
    
    The aspect_ratio_threshold helps distinguish thin rivers from
    square noise blobs - rivers have low min(w,h)/max(w,h) ratios.
    """
    num_labels, labels, stats, _ = cv2.connectedComponentsWithStats(mask, connectivity=8)
    out = np.zeros_like(mask)

    for lab in range(1, num_labels):
        w = stats[lab, cv2.CC_STAT_WIDTH]
        h = stats[lab, cv2.CC_STAT_HEIGHT]
        area = stats[lab, cv2.CC_STAT_AREA]
        length = max(w, h)
        aspect = min(w, h) / max(w, h) if max(w, h) > 0 else 1.0

        is_large = area >= min_area
        is_elongated_river = length >= min_length and aspect <= aspect_ratio_threshold
        is_substantial_long = length >= min_length and area >= min_area * 0.3

        if is_large or is_elongated_river or is_substantial_long:
            out[labels == lab] = 255

    return out


def auto_hsv_thresholds(img_bgr: np.ndarray, b_dom: int, b_min: int) -> tuple[int, int, int, int]:
    """
    Auto-learn HSV thresholds for water-ish blue from the image.
    Returns: (hmin, hmax, smin, vmin)
    """
    b, g, r = cv2.split(img_bgr)

    cand = (b >= b_min) & (b.astype(np.int16) - np.maximum(r, g).astype(np.int16) >= b_dom)

    if int(cand.sum()) < 200:
        return 75, 160, 30, 25

    hsv = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2HSV)
    h = hsv[:, :, 0][cand]
    s = hsv[:, :, 1][cand]
    v = hsv[:, :, 2][cand]

    hmin = int(np.percentile(h, 5))
    hmax = int(np.percentile(h, 95))
    smin = int(np.percentile(s, 10))
    vmin = int(np.percentile(v, 10))

    hmin = max(0, hmin - 10)
    hmax = min(179, hmax + 10)
    smin = max(0, smin - 10)
    vmin = max(0, vmin - 10)

    hmin = min(hmin, 100)
    hmax = max(hmax, 120)

    return hmin, hmax, smin, vmin


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--in", dest="inp", required=True)
    ap.add_argument("--out", required=True)

    # Manual HSV thresholds (used if --auto_color is NOT set)
    ap.add_argument("--hmin", type=int, default=75)
    ap.add_argument("--hmax", type=int, default=160)
    ap.add_argument("--smin", type=int, default=30)
    ap.add_argument("--vmin", type=int, default=25)

    # Auto calibration + dominance mask
    ap.add_argument("--auto_color", action="store_true")
    ap.add_argument("--b_dom", type=int, default=20)
    ap.add_argument("--b_min", type=int, default=30)

    # Cleanup
    ap.add_argument("--cleanup", action="store_true")
    ap.add_argument("--close_k", type=int, default=7)
    ap.add_argument("--close_iter", type=int, default=3)
    ap.add_argument("--open_k", type=int, default=3)
    ap.add_argument("--open_iter", type=int, default=0, help="Set 0 to avoid deleting thin streams")

    # Filtering (important)
    ap.add_argument("--min_area", type=int, default=150)
    ap.add_argument("--min_length", type=int, default=25)

    # Hole filling options
    ap.add_argument("--fill_holes", action="store_true", 
                    help="Fill small holes inside water regions (improved method)")
    ap.add_argument("--max_hole_area", type=int, default=500,
                    help="Maximum hole area to fill (prevents filling large gaps)")
    ap.add_argument("--legacy_fill", action="store_true",
                    help="Use old flood-fill method (not recommended)")

    args = ap.parse_args()

    img = cv2.imread(args.inp, cv2.IMREAD_COLOR)
    if img is None:
        raise FileNotFoundError(f"Could not read image: {args.inp}")

    if args.auto_color:
        hmin, hmax, smin, vmin = auto_hsv_thresholds(img, b_dom=args.b_dom, b_min=args.b_min)
    else:
        hmin, hmax, smin, vmin = args.hmin, args.hmax, args.smin, args.vmin

    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    lower = np.array([hmin, smin, vmin], dtype=np.uint8)
    upper = np.array([hmax, 255, 255], dtype=np.uint8)

    mask_hsv = cv2.inRange(hsv, lower, upper)

    b, g, r = cv2.split(img)
    blue_dom = (b.astype(np.int16) - np.maximum(r, g).astype(np.int16) >= args.b_dom) & (b >= args.b_min)
    mask_dom = (blue_dom.astype(np.uint8) * 255)

    mask = cv2.bitwise_or(mask_hsv, mask_dom)

    if args.cleanup:
        ck = args.close_k if args.close_k % 2 == 1 else args.close_k + 1
        close_kernel = np.ones((ck, ck), np.uint8)
        mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, close_kernel, iterations=args.close_iter)

        if args.open_iter > 0:
            ok = args.open_k if args.open_k % 2 == 1 else args.open_k + 1
            open_kernel = np.ones((ok, ok), np.uint8)
            mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, open_kernel, iterations=args.open_iter)

    mask = filter_components(mask, min_area=args.min_area, min_length=args.min_length)

    if args.fill_holes:
        if args.legacy_fill:
            mask = fill_holes_legacy(mask)
        else:
            mask = fill_holes_per_component(mask, max_hole_area=args.max_hole_area)

    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    cv2.imwrite(args.out, mask)
    print(f"Saved mask: {args.out}")
    if args.auto_color:
        print(f"Auto thresholds used: h=[{hmin},{hmax}] smin={smin} vmin={vmin}")


if __name__ == "__main__":
    main()
