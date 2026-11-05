#!/usr/bin/env python3
"""
Pylive Music — générateur des ressources graphiques de marque.

Source unique de vérité pour :
  * l'icône adaptative Android (avant-plan, arrière-plan, couche monochrome Android 13+) ;
  * les icônes PNG de secours (mipmap-*dpi) ;
  * le logo vectoriel utilisé dans l'interface ;
  * l'icône de notification ;
  * les exports de la marque (SVG + PNG) livrés dans branding/ et docs/.

Palette imposée :
  violet #B80DB8 · blanc #FFFFFF · jaune #F5D328

Le monogramme est un « P » monoliné à extrémités arrondies dont le contre-poinçon
contient un point jaune (tête de note / sillon de disque). Le dessin est
entièrement géométrique : aucune police n'est nécessaire, donc le rendu est
identique d'une machine à l'autre.

Usage :  python3 scripts/branding/generate_brand_assets.py
Aucune dépendance obligatoire ; Pillow est requis pour les PNG (pip install Pillow).
"""

from __future__ import annotations

import math
import shutil
from pathlib import Path

VIOLET = (0xB8, 0x0D, 0xB8)
VIOLET_DARK = (0x93, 0x0A, 0x93)   # nuance technique, uniquement pour de légers dégradés
WHITE = (0xFF, 0xFF, 0xFF)
YELLOW = (0xF5, 0xD3, 0x28)

HEX_VIOLET = "#FFB80DB8"
HEX_VIOLET_DEEP = "#FF930A93"
HEX_WHITE = "#FFFFFFFF"
HEX_YELLOW = "#FFF5D328"

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app" / "src" / "main" / "res"
BRANDING = ROOT / "branding"
DOCS = ROOT / "docs"

# ---------------------------------------------------------------------------
# Géométrie du monogramme, exprimée dans un repère « unités de marque ».
# ---------------------------------------------------------------------------
STROKE = 11.0
STEM_X = 8.0
TOP_Y = 6.0
BOTTOM_Y = 58.0
BOWL_CX = 26.0
BOWL_CY = 22.0
BOWL_R = 16.0
DOT_R = 7.0
MARK_W = (BOWL_CX + BOWL_R + STROKE / 2) - (STEM_X - STROKE / 2)   # 45.0
MARK_H = (BOTTOM_Y + STROKE / 2) - (TOP_Y - STROKE / 2)            # 63.0


def mark_transform(size: float, scale: float):
    """Retourne (échelle, dx, dy) pour centrer le monogramme dans un carré `size`."""
    ox = STEM_X - STROKE / 2
    oy = TOP_Y - STROKE / 2
    s = scale
    dx = (size - MARK_W * s) / 2 - ox * s
    dy = (size - MARK_H * s) / 2 - oy * s
    return s, dx, dy


def pt(x: float, y: float, size: float, scale: float):
    s, dx, dy = mark_transform(size, scale)
    return (x * s + dx, y * s + dy)


def path_data(size: float, scale: float) -> str:
    """Trace du P monoliné en coordonnées du viewport (108 par défaut)."""
    p = lambda x, y: pt(x, y, size, scale)
    x, top, bottom, cx, cy, r = STEM_X, TOP_Y, BOTTOM_Y, BOWL_CX, BOWL_CY, BOWL_R
    a = p(x, bottom)
    b = p(x, top)
    c = p(cx, top)
    d = p(cx, top + 2 * r)
    e = p(x, top + 2 * r)
    f = lambda v: f"{v:.3f}".rstrip("0").rstrip(".")
    arc = f"A{f(r)} {f(r)} 0 0 1 {f(d[0])} {f(d[1])}"
    return (f"M{f(a[0])},{f(a[1])} L{f(b[0])},{f(b[1])} L{f(c[0])},{f(c[1])} "
            f"{arc} L{f(e[0])},{f(e[1])}")


def circle_path(cx: float, cy: float, r: float, size: float, scale: float) -> str:
    c = pt(cx, cy, size, scale)
    rr = r * mark_transform(size, scale)[0]
    f = lambda v: f"{v:.3f}".rstrip("0").rstrip(".")
    return (f"M{f(c[0] - rr)},{f(c[1])} "
            f"a{f(rr)},{f(rr)} 0 1 0 {f(2 * rr)},0 "
            f"a{f(rr)},{f(rr)} 0 1 0 {f(-2 * rr)},0 Z")


def vector_drawable(width_dp: int, height_dp: int, body: str) -> str:
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Pylive Music — marque dessinée pour ce projet (aucune ressource tierce). -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{width_dp}dp"\n'
        f'    android:height="{height_dp}dp"\n'
        f'    android:viewportWidth="108"\n'
        f'    android:viewportHeight="108">\n'
        f"{body}</vector>\n"
    )


def monogram_paths(size: float = 108.0, scale: float = 58.0 / MARK_H,
                   stroke_color: str = HEX_WHITE, dot_color: str = HEX_YELLOW) -> str:
    d = path_data(size, scale)
    dot = circle_path(BOWL_CX, BOWL_CY, DOT_R, size, scale)
    sw = STROKE * mark_transform(size, scale)[0]
    return (
        '    <path\n'
        f'        android:pathData="{d}"\n'
        '        android:fillColor="@android:color/transparent"\n'
        f'        android:strokeColor="{stroke_color}"\n'
        f'        android:strokeWidth="{sw:.3f}"\n'
        '        android:strokeLineCap="round"\n'
        '        android:strokeLineJoin="round" />\n'
        '    <path\n'
        f'        android:fillColor="{dot_color}"\n'
        f'        android:pathData="{dot}" />\n'
    )


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(f"  écrit {path.relative_to(ROOT)}")


# ---------------------------------------------------------------------------
# 1. Ressources vectorielles Android
# ---------------------------------------------------------------------------
def android_vectors() -> None:
    print("Ressources vectorielles :")
    # Avant-plan de l'icône adaptative : monogramme blanc + point jaune.
    # Le viewport 108 laisse 18dp de marge de sécurité de chaque côté.
    write(RES / "drawable" / "ic_launcher_foreground.xml",
          vector_drawable(108, 108, monogram_paths()))

    # Couche monochrome (icônes thématiques Android 13+) : silhouette blanche unie.
    mono = (
        '    <path\n'
        f'        android:pathData="{path_data(108.0, 58.0 / MARK_H)}"\n'
        '        android:fillColor="@android:color/transparent"\n'
        '        android:strokeColor="#FFFFFFFF"\n'
        f'        android:strokeWidth="{STROKE * (58.0 / MARK_H):.3f}"\n'
        '        android:strokeLineCap="round"\n'
        '        android:strokeLineJoin="round" />\n'
        '    <path\n'
        '        android:fillColor="#FFFFFFFF"\n'
        f'        android:pathData="{circle_path(BOWL_CX, BOWL_CY, DOT_R, 108.0, 58.0 / MARK_H)}" />\n'
    )
    write(RES / "drawable" / "ic_launcher_monochrome.xml", vector_drawable(108, 108, mono))

    # Marque pour fond clair : P violet + point jaune.
    write(RES / "drawable" / "ic_brand_mark.xml",
          vector_drawable(48, 48, monogram_paths(stroke_color=HEX_VIOLET)))
    # Variante pour fond violet (en-têtes, lecteur) : P blanc + point jaune.
    write(RES / "drawable" / "ic_brand_mark_light.xml",
          vector_drawable(48, 48, monogram_paths()))

    # Icône de notification : note blanche unie sur transparent, 24dp, zone sûre respectée.
    note = (
        '    <path\n'
        '        android:fillColor="#FFFFFFFF"\n'
        '        android:pathData="M13.5,4 L13.5,15.05 A3.6,3.1 0 1 1 10.8,13.4 '
        'A3.55,3.05 0 0 1 11.7,13.55 L11.7,6.9 L5.6,8.35 L5.6,18.2 '
        'A3.6,3.1 0 1 1 2.25,16.5 A3.5,3.0 0 0 1 3.8,16.7 L3.8,7.05 Z" />\n'
    )
    write(RES / "drawable" / "ic_notification.xml", vector_drawable(24, 24, note))

    # Fond de l'icône adaptative : violet uni (les dégradés subtils sont réservés
    # aux exports marketing ; le lanceur reste net et sans surprise de rendu).
    write(RES / "drawable" / "ic_launcher_background.xml",
          '<?xml version="1.0" encoding="utf-8"?>\n'
          "<!-- Pylive Music — fond violet de l'icône adaptative. -->\n"
          '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
          '    android:width="108dp"\n'
          '    android:height="108dp"\n'
          '    android:viewportWidth="108"\n'
          '    android:viewportHeight="108">\n'
          '    <path android:fillColor="#FFB80DB8" android:pathData="M0,0h108v108h-108z" />\n'
          "</vector>\n")

    # Icônes adaptatives (API 26+) et variante monochrome (API 33+).
    adaptive = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@drawable/ic_launcher_background" />\n'
        '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
        '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
        "</adaptive-icon>\n"
    )
    write(RES / "mipmap-anydpi-v26" / "ic_launcher.xml", adaptive)
    write(RES / "mipmap-anydpi-v26" / "ic_launcher_round.xml", adaptive)
    write(RES / "mipmap-anydpi-v33" / "ic_launcher.xml", adaptive)
    write(RES / "mipmap-anydpi-v33" / "ic_launcher_round.xml", adaptive)

    # Marque de démarrage (splash) : dessin 108dp, fond géré par le thème.
    write(RES / "drawable" / "brand_splash.xml",
          vector_drawable(108, 108, monogram_paths()))


# ---------------------------------------------------------------------------
# 2. Icônes PNG de secours + exports de marque (Pillow)
# ---------------------------------------------------------------------------
def _brush(draw, pts, width, color):
    draw.line(pts, fill=color, width=int(round(width)), joint="curve")
    r = width / 2
    for (x, y) in (pts[0], pts[-1]):
        draw.ellipse([x - r, y - r, x + r, y + r], fill=color)


def _bowl_points(cx, cy, r, steps=72):
    # demi-cercle de droite, du haut vers le bas (sens horaire)
    return [(cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a)))
            for a in range(-90, 91, max(1, 180 // steps))]


def _superellipse_mask(side, exponent=5.0, samples=4):
    """Masque booléen d'une squircle (superellipse) centrée, de côté `side`."""
    from PIL import Image, ImageDraw

    big = side * samples
    img = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(img)
    n = 720
    pts = []
    for i in range(n):
        t = 2 * math.pi * i / n
        ct, st = math.cos(t), math.sin(t)
        x = math.copysign(abs(ct) ** (2.0 / exponent), ct)
        y = math.copysign(abs(st) ** (2.0 / exponent), st)
        pts.append((big / 2 + x * big / 2, big / 2 + y * big / 2))
    d.polygon(pts, fill=255)
    img = img.resize((side, side), Image.LANCZOS)
    return img.point(lambda v: 255 if v > 127 else 0)


def _mark_masks(canvas_side, color_stroke, color_dot, scale_mark=0.58, samples=4):
    """Construit les deux calques (trait + pastille) du monogramme, en RGBA.

    Le tracé est l'union exacte de capsules (segments arrondis) et d'un demi-anneau,
    ce qui reproduit fidèlement le `path` vectoriel sans artefact de jointure.
    """
    from PIL import Image, ImageDraw

    side = canvas_side * samples
    s = (scale_mark * side) / MARK_H
    ox = (STEM_X - STROKE / 2)
    oy = (TOP_Y - STROKE / 2)
    dx = (side - MARK_W * s) / 2 - ox * s
    dy = (side - MARK_H * s) / 2 - oy * s
    P = lambda x, y: (x * s + dx, y * s + dy)
    sw = STROKE * s

    stroke_img = Image.new("L", (side, side), 0)
    d = ImageDraw.Draw(stroke_img)

    def capsule(p, q):
        (x1, y1), (x2, y2) = p, q
        d.line([x1, y1, x2, y2], fill=255, width=int(round(sw)))
        r = sw / 2
        for (x, y) in (p, q):
            d.ellipse([x - r, y - r, x + r, y + r], fill=255)

    capsule(P(STEM_X, BOTTOM_Y), P(STEM_X, TOP_Y))               # hampe
    capsule(P(STEM_X, TOP_Y), P(BOWL_CX, TOP_Y))                 # épaulement haut
    capsule(P(STEM_X, TOP_Y + 2 * BOWL_R), P(BOWL_CX, TOP_Y + 2 * BOWL_R))  # barre

    # Demi-anneau droit : disque extérieur moins disque intérieur, moitié droite.
    c = P(BOWL_CX, BOWL_CY)
    ro, ri = (BOWL_R + STROKE / 2) * s, (BOWL_R - STROKE / 2) * s
    ring = Image.new("L", (side, side), 0)
    rd = ImageDraw.Draw(ring)
    rd.ellipse([c[0] - ro, c[1] - ro, c[0] + ro, c[1] + ro], fill=255)
    rd.ellipse([c[0] - ri, c[1] - ri, c[0] + ri, c[1] + ri], fill=0)
    right = Image.new("L", (side, side), 0)
    ImageDraw.Draw(right).rectangle([c[0], 0, side, side], fill=255)
    ring = Image.composite(ring, Image.new("L", (side, side), 0), right)
    stroke_img = _mask_union(stroke_img, ring)

    dot_img = Image.new("L", (side, side), 0)
    r = DOT_R * s
    ImageDraw.Draw(dot_img).ellipse([c[0] - r, c[1] - r, c[0] + r, c[1] + r], fill=255)

    down = lambda m: m.resize((canvas_side, canvas_side), Image.LANCZOS)
    stroke_layer = Image.new("RGBA", (canvas_side, canvas_side), tuple(color_stroke[:3]) + (0,))
    stroke_layer.putalpha(down(stroke_img))
    dot_layer = Image.new("RGBA", (canvas_side, canvas_side), tuple(color_dot[:3]) + (0,))
    dot_layer.putalpha(down(dot_img))
    return stroke_layer, dot_layer


def _mask_union(a, b):
    from PIL import ImageChops

    return ImageChops.lighter(a, b)


def _mark_on(canvas, color_stroke, color_dot, scale_mark=0.58):
    """Applique le monogramme centré sur une image Pillow carrée (RGBA)."""
    side = canvas.size[0]
    stroke_layer, dot_layer = _mark_masks(min(canvas.size), color_stroke, color_dot, scale_mark)
    if canvas.size[0] != side:
        stroke_layer = stroke_layer.resize(canvas.size, Image.LANCZOS)
        dot_layer = dot_layer.resize(canvas.size, Image.LANCZOS)
    canvas.alpha_composite(stroke_layer)
    canvas.alpha_composite(dot_layer)



def _rounded(draw, box, radius, fill):
    draw.rounded_rectangle(box, radius=radius, fill=fill)


def png_icon(size, background="rounded"):
    from PIL import Image

    S = size * 4  # suréchantillonnage pour un anti-aliasing propre
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    from PIL import ImageDraw

    d = ImageDraw.Draw(img)
    if background == "circle":
        d.ellipse([0, 0, S - 1, S - 1], fill=VIOLET + (255,))
    elif background == "rounded":
        _rounded(d, [0, 0, S - 1, S - 1], int(S * 0.235), VIOLET + (255,))
    else:
        d.rectangle([0, 0, S, S], fill=VIOLET + (255,))
    _mark_on(img, WHITE + (255,), YELLOW + (255,), scale_mark=0.60)
    return img.resize((size, size), Image.LANCZOS)


def svg_logo() -> str:
    """Logo carré 512×512 : fond violet arrondi, monogramme blanc, point jaune."""
    s, dx, dy = mark_transform(512.0, 512.0 * 0.60 / MARK_H)
    f = lambda v: f"{v:.2f}".rstrip("0").rstrip(".")
    x, top, bottom, cx, cy, r = STEM_X, TOP_Y, BOTTOM_Y, BOWL_CX, BOWL_CY, BOWL_R
    P = lambda a, b: (f(a * s + dx), f(b * s + dy))
    a, b, c = P(x, bottom), P(x, top), P(cx, top)
    dp = P(cx, top + 2 * r)
    e = P(x, top + 2 * r)
    dot = P(cx, cy)
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512" role="img" aria-label="Pylive Music">
  <title>Pylive Music</title>
  <rect width="512" height="512" rx="120" fill="{HEX_VIOLET}"/>
  <path d="M{a[0]},{a[1]} L{b[0]},{b[1]} L{c[0]},{c[1]} A{f(r * s)},{f(r * s)} 0 0 1 {dp[0]},{dp[1]} L{e[0]},{e[1]}"
        fill="none" stroke="{HEX_WHITE}" stroke-width="{f(STROKE * s)}"
        stroke-linecap="round" stroke-linejoin="round"/>
  <circle cx="{dot[0]}" cy="{dot[1]}" r="{f(DOT_R * s)}" fill="{HEX_YELLOW}"/>
</svg>
"""


def svg_wordmark() -> str:
    """Signature horizontale : pastille + « PYLIVE » / « MUSIC »."""
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="960" height="280" viewBox="0 0 960 280" role="img" aria-label="Pylive Music">
  <title>Pylive Music</title>
  <rect width="960" height="280" rx="0" fill="{HEX_WHITE}"/>
  <rect x="40" y="40" width="200" height="200" rx="48" fill="{HEX_VIOLET}"/>
  <g transform="translate(40 40) scale(1.851852)">
    <path d="M8,58 L8,6 L26,6 A16,16 0 0 1 26,38 L8,38"
          fill="none" stroke="{HEX_WHITE}" stroke-width="11"
          stroke-linecap="round" stroke-linejoin="round"
          transform="translate(-2.4 -1.1)"/>
    <circle cx="24" cy="21.5" r="8" fill="{HEX_YELLOW}"/>
  </g>
  <text x="280" y="132" font-family="Roboto, Arial, Helvetica, sans-serif" font-size="92"
        font-weight="700" letter-spacing="2" fill="{HEX_VIOLET}">PYLIVE</text>
  <text x="284" y="196" font-family="Roboto, Arial, Helvetica, sans-serif" font-size="44"
        font-weight="500" letter-spacing="10" fill="#FF4A4A56">MUSIC</text>
  <rect x="284" y="214" width="120" height="10" rx="5" fill="{HEX_YELLOW}"/>
</svg>
"""


def write_pngs() -> None:
    print("Icônes PNG et exports :")
    from PIL import Image

    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for bucket, px in sizes.items():
        png_icon(px).save(RES / f"mipmap-{bucket}" / "ic_launcher.png")
        png_icon(px, background="circle").save(RES / f"mipmap-{bucket}" / "ic_launcher_round.png")
        print(f"  écrit app/src/main/res/mipmap-{bucket}/ic_launcher.png (+ round)")

    BRANDING.mkdir(parents=True, exist_ok=True)
    DOCS.mkdir(parents=True, exist_ok=True)
    png_icon(512).save(BRANDING / "logo-512.png")
    png_icon(512).save(DOCS / "pylive-logo-512.png")
    png_icon(1024).save(BRANDING / "icon-playstore-1024.png")
    print("  écrit branding/logo-512.png, branding/icon-playstore-1024.png")

    # Planche de contrôle de la lisibilité à petite taille (48 px réel).
    sheet = Image.new("RGBA", (900, 420), (247, 245, 250, 255))
    small = png_icon(48)
    shot = Image.new("RGBA", (900, 420), (247, 245, 250, 255))
    shot.alpha_composite(png_icon(192), (40, 40))
    shot.alpha_composite(png_icon(96), (280, 40))
    shot.alpha_composite(small, (420, 40))
    shot.alpha_composite(png_icon(144, background="circle"), (40, 280))
    shot.alpha_composite(png_icon(48, background="circle"), (240, 280))
    shot.alpha_composite(png_icon(72, background=None), (330, 280))
    shot.save(DOCS / "icones-apercu.png")
    print("  écrit docs/icones-apercu.png")


def main() -> None:
    android_vectors()
    write(BRANDING / "logo.svg", svg_logo())
    write(BRANDING / "signature.svg", svg_wordmark())
    write(BRANDING / "logo-dark.svg", svg_logo().replace('fill="#FFFFFFFF"', 'fill="#FFFFFFFF"'))
    try:
        import PIL  # noqa: F401
    except ImportError:
        print("Pillow absent : PNG non régénérés (XML vectoriels et SVG écrits).")
        return
    write_pngs()

    # Copie de travail éventuelle d'un ancien dossier de ressources : on nettoie
    # les fichiers *anydpi-v33* obsolètes qui auraient pu être générés avant.
    legacy = RES / "mipmap-anydpi-v33" / "ic_launcher_background.xml"
    if legacy.exists():
        legacy.unlink()


if __name__ == "__main__":
    main()
