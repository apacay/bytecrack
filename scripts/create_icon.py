#!/usr/bin/env python3
"""Genera iconos de la app a partir de la imagen fuente."""
from pathlib import Path

from PIL import Image

# Rutas
SRC_IMAGE = Path(r"C:\Users\aripa\Downloads\6a05c609-53b9-439d-a9b4-d5e00c7a2262.png")
RES_DIR = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res"

# Tamaños para Android
# Mipmap (icono tradicional): mdpi=48, hdpi=72, xhdpi=96, xxhdpi=144, xxxhdpi=192
# Adaptive icon foreground (108dp): mdpi=108, hdpi=162, xhdpi=216, xxhdpi=324, xxxhdpi=432
MIPMAP_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

FOREGROUND_SIZES = {
    "drawable-mdpi": 108,
    "drawable-hdpi": 162,
    "drawable-xhdpi": 216,
    "drawable-xxhdpi": 324,
    "drawable-xxxhdpi": 432,
}


def remove_white_corners(img: Image.Image, threshold: int = 250) -> Image.Image:
    """Convierte píxeles blancos/near-white en transparentes."""
    data = img.getdata()
    new_data = []
    for item in data:
        r, g, b, a = item
        if r >= threshold and g >= threshold and b >= threshold:
            new_data.append((r, g, b, 0))
        else:
            new_data.append(item)
    img.putdata(new_data)
    return img


def main():
    img = Image.open(SRC_IMAGE).convert("RGBA")
    img = remove_white_corners(img)

    # 1. Versión 1024x1024 para Play Store / uso general
    store_dir = Path(__file__).resolve().parent.parent / "store"
    store_dir.mkdir(exist_ok=True)
    icon_1024 = img.resize((1024, 1024), Image.Resampling.LANCZOS)
    icon_1024.save(store_dir / "ic_launcher_1024.png")
    print(f"[OK] Creado: {store_dir / 'ic_launcher_1024.png'} (1024x1024)")

    # 2. Mipmap para icono tradicional (legacy)
    for folder, size in MIPMAP_SIZES.items():
        folder_path = RES_DIR / folder
        folder_path.mkdir(exist_ok=True)
        resized = img.resize((size, size), Image.Resampling.LANCZOS)
        resized.save(folder_path / "ic_launcher.png")
        resized.save(folder_path / "ic_launcher_round.png")
        print(f"[OK] {folder}/ic_launcher.png ({size}x{size})")

    # 3. Foreground para adaptive icon (API 26+)
    for folder, size in FOREGROUND_SIZES.items():
        folder_path = RES_DIR / folder
        folder_path.mkdir(exist_ok=True)
        resized = img.resize((size, size), Image.Resampling.LANCZOS)
        resized.save(folder_path / "ic_launcher_foreground.png")
        print(f"[OK] {folder}/ic_launcher_foreground.png ({size}x{size})")

    print("\nIconos generados correctamente.")


if __name__ == "__main__":
    main()
