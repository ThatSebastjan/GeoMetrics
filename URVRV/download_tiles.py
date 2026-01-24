import os
import math
import time
import random
import argparse
import urllib.request


def deg2num(lat_deg, lon_deg, zoom):
    lat_rad = math.radians(lat_deg)
    n = 2.0 ** zoom
    xtile = int((lon_deg + 180.0) / 360.0 * n)
    ytile = int((1.0 - math.asinh(math.tan(lat_rad)) / math.pi) / 2.0 * n)
    return xtile, ytile


def download(url, out_path, user_agent):
    req = urllib.request.Request(url, headers={"User-Agent": user_agent})

    with urllib.request.urlopen(req, timeout=30) as r:
        data = r.read()
    
    with open(out_path, "wb") as f:
        f.write(data)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--z", type=int, default=15)
    ap.add_argument("--count", type=int, default=200)
    ap.add_argument("--minlat", type=float, required=True)
    ap.add_argument("--minlon", type=float, required=True)
    ap.add_argument("--maxlat", type=float, required=True)
    ap.add_argument("--maxlon", type=float, required=True)
    ap.add_argument("--outdir", default="images")
    ap.add_argument("--sleep", type=float, default=1.0)
    ap.add_argument("--tile_url", default="https://tile.openstreetmap.org/{z}/{x}/{y}.png")
    args = ap.parse_args()

    os.makedirs(args.outdir, exist_ok=True)

    x1, y2 = deg2num(args.minlat, args.minlon, args.z)
    x2, y1 = deg2num(args.maxlat, args.maxlon, args.z)

    xmin, xmax = sorted((x1, x2))
    ymin, ymax = sorted((y1, y2))

    if (xmax - xmin + 1) * (ymax - ymin + 1) > 200000:
        raise SystemExit("Tile range too large. Reduce bbox or zoom.")
    
    
    for i in range(1, args.count + 1):
        x = random.randint(xmin, xmax)
        y = random.randint(ymin, ymax)

        url = args.tile_url.format(z=args.z, x=x, y=y)
        out_path = os.path.join(args.outdir, f"img_{i:03d}.png")

        try:
            download(url, out_path, args.user_agent)
            print(f"Downloaded img_{i:03d}.png")
        except Exception as e:
            print("Failed:", e)

        time.sleep(args.sleep)


if __name__ == "__main__":
    main()
