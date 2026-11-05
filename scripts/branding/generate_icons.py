#!/usr/bin/env python3
"""
Pylive Music — jeu d'icônes maison.

Chaque icône est décrite géométriquement une seule fois, puis produite en deux
formats cohérents :
  * un vector drawable Android (app/src/main/res/drawable/ic_*.xml) ;
  * un aperçu PNG (docs/icones-systeme.png) pour contrôler la lisibilité
    optique à 24 px réels et à taille agrandie.

Aucune ressource tierce n'est copiée : les tracés sont dessinés ici, sur une
grille de 24 × 24, trait de 2 px, extrémités et jointures arrondies.

Usage : python3 scripts/branding/generate_icons.py
"""

from __future__ import annotations

import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable"
DOCS = ROOT / "docs"

STROKE = 2.0
GRID = 24.0


# ---------------------------------------------------------------------------
# Mini-moteur de tracé
# ---------------------------------------------------------------------------
class Path:
    """Suite de sous-tracés composés de segments linéaires ou de Bézier cubiques."""

    def __init__(self):
        self.subpaths: list[list[tuple]] = []
        self._cur = None

    # --- construction -------------------------------------------------------
    def move(self, x, y):
        self._cur = [("M", float(x), float(y))]
        self.subpaths.append(self._cur)
        return self

    def line(self, x, y):
        self._cur.append(("L", float(x), float(y)))
        return self

    def cubic(self, x1, y1, x2, y2, x, y):
        self._cur.append(("C", float(x1), float(y1), float(x2), float(y2), float(x), float(y)))
        return self

    def quad(self, x1, y1, x, y):
        # conversion exacte en cubique
        px, py = self._cur[-1][-2], self._cur[-1][-1]
        return self.cubic(px + 2 / 3 * (x1 - px), py + 2 / 3 * (y1 - py),
                          x + 2 / 3 * (x1 - x), y + 2 / 3 * (y1 - y), x, y)

    def arc(self, cx, cy, r, start_deg, end_deg):
        """Arc circulaire converti en segments cubiques (max 90° par segment)."""
        total = end_deg - start_deg
        steps = max(1, int(math.ceil(abs(total) / 90.0)))
        step = total / steps
        a0 = math.radians(start_deg)
        for _ in range(steps):
            a1 = a0 + math.radians(step)
            k = 4 / 3 * math.tan((a1 - a0) / 4)
            x0, y0 = cx + r * math.cos(a0), cy + r * math.sin(a0)
            x1, y1 = cx + r * math.cos(a1), cy + r * math.sin(a1)
            self.cubic(x0 - k * r * math.sin(a0), y0 + k * r * math.cos(a0),
                       x1 + k * r * math.sin(a1), y1 - k * r * math.cos(a1), x1, y1)
            a0 = a1
        return self

    def close(self):
        self._cur.append(("Z",))
        return self

    def poly(self, pts, closed=False):
        self.move(*pts[0])
        for p in pts[1:]:
            self.line(*p)
        if closed:
            self.close()
        return self

    def rounded_rect(self, x0, y0, x1, y1, r):
        k = 0.5523 * r
        return (self.move(x0 + r, y0)
                .line(x1 - r, y0).cubic(x1 - r + k, y0, x1, y0 + r - k, x1, y0 + r)
                .line(x1, y1 - r).cubic(x1, y1 - r + k, x1 - r + k, y1, x1 - r, y1)
                .line(x0 + r, y1).cubic(x0 + r - k, y1, x0, y1 - r + k, x0, y1 - r)
                .line(x0, y0 + r).cubic(x0, y0 + r - k, x0 + r - k, y0, x0 + r, y0)
                .close())

    def circle(self, cx, cy, r):
        return (self.move(cx - r, cy)
                .arc(cx, cy, r, 180, 360)
                .arc(cx, cy, r, 0, 180)
                .close())

    # --- sorties -----------------------------------------------------------
    def d(self) -> str:
        out = []
        for sp in self.subpaths:
            for seg in sp:
                if seg[0] == "M":
                    out.append(f"M{seg[1]:.2f},{seg[2]:.2f}")
                elif seg[0] == "L":
                    out.append(f"L{seg[1]:.2f},{seg[2]:.2f}")
                elif seg[0] == "C":
                    out.append("C{:.2f},{:.2f},{:.2f},{:.2f},{:.2f},{:.2f}".format(*seg[1:]))
                elif seg[0] == "Z":
                    out.append("Z")
        return " ".join(out)

    def sample(self, step=0.35):
        """Polylignes échantillonnées (pour l'aperçu PNG)."""
        polys = []
        for sp in self.subpaths:
            cur = []
            x = y = 0.0
            start = None
            for seg in sp:
                if seg[0] == "M":
                    if len(cur) > 1:
                        polys.append(cur)
                    x, y = seg[1], seg[2]
                    start = (x, y)
                    cur = [(x, y)]
                elif seg[0] == "L":
                    x, y = seg[1], seg[2]
                    cur.append((x, y))
                elif seg[0] == "C":
                    x0, y0 = x, y
                    x1, y1, x2, y2, x3, y3 = seg[1:]
                    dist = math.hypot(x1 - x0, y1 - y0) + math.hypot(x2 - x1, y2 - y1) + math.hypot(x3 - x2, y3 - y2)
                    n = max(2, int(dist / step))
                    for i in range(1, n + 1):
                        t = i / n
                        mt = 1 - t
                        px = mt**3 * x0 + 3 * mt**2 * t * x1 + 3 * mt * t**2 * x2 + t**3 * x3
                        py = mt**3 * y0 + 3 * mt**2 * t * y1 + 3 * mt * t**2 * y2 + t**3 * y3
                        cur.append((px, py))
                    x, y = x3, y3
                elif seg[0] == "Z":
                    if start:
                        cur.append(start)
            if len(cur) > 1:
                polys.append(cur)
        return polys

    def points_inside(self):
        """Sommets utiles pour un remplissage (polygones fermés uniquement)."""
        out = []
        for sp in self.subpaths:
            pts = []
            for seg in sp:
                if seg[0] in ("M", "L"):
                    pts.append((seg[1], seg[2]))
            if len(pts) > 2:
                out.append(pts)
        return out


class Icon:
    def __init__(self, name, stroke=None, fill=None, mirror=False):
        self.name = name
        self.stroke_paths: list[Path] = stroke or []
        self.fill_paths: list[Path] = fill or []
        self.mirror = mirror

    def xml(self) -> str:
        parts = [
            '<?xml version="1.0" encoding="utf-8"?>',
            "<!-- Pylive Music — icône dessinée pour ce projet (aucune ressource tierce). -->",
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
            '    android:width="24dp"',
            '    android:height="24dp"',
            '    android:viewportWidth="24"',
            '    android:viewportHeight="24"',
            (f'    android:autoMirrored="true"' if self.mirror else '    android:tint="?android:attr/colorControlNormal"')
            .replace('    android:tint="?android:attr/colorControlNormal"', '    android:tint="#FF1A1018"'),
            ">",
        ]
        for p in self.stroke_paths:
            parts += [
                "    <path",
                f'        android:pathData="{p.d()}"',
                '        android:fillColor="@android:color/transparent"',
                '        android:strokeColor="#FF1A1018"',
                f'        android:strokeWidth="{STROKE}"',
                '        android:strokeLineCap="round"',
                '        android:strokeLineJoin="round" />',
            ]
        for p in self.fill_paths:
            parts += [
                "    <path",
                f'        android:pathData="{p.d()}"',
                '        android:fillColor="#FF1A1018" />',
            ]
        parts.append("</vector>")
        # Le teintage est laissé à Icon(painterResource(...), tint = ...) côté Compose.
        return "\n".join(p.replace('    android:tint="#FF1A1018"', '    android:tint="#FF000000"') for p in parts) + "\n"


# ---------------------------------------------------------------------------
# Aides de dessin
# ---------------------------------------------------------------------------
def S():
    return Path()


def arrow_head(p: Path, tip, direction, size=3.0):
    """Pointe de flèche en V, `direction` = 'right' | 'left' | 'up' | 'down'."""
    x, y = tip
    if direction == "right":
        p.move(x - size, y - size * 0.8).line(x, y).line(x - size, y + size * 0.8)
    elif direction == "left":
        p.move(x + size, y - size * 0.8).line(x, y).line(x + size, y + size * 0.8)
    elif direction == "up":
        p.move(x - size * 0.8, y + size).line(x, y).line(x + size * 0.8, y + size)
    else:
        p.move(x - size * 0.8, y - size).line(x, y).line(x + size * 0.8, y - size)
    return p


def gear(teeth=8, r_out=9.4, r_in=6.0, cx=12, cy=12, tooth_w=0.38):
    """Engrenage : anneau + dents trapézoïdales, tracé d'un seul tenant."""
    p = S()
    step = 2 * math.pi / teeth
    for i in range(teeth):
        a = i * step
        half = step * tooth_w
        pts = [
            (cx + (r_in + 0.6) * math.cos(a - half * 1.5), cy + (r_in + 0.6) * math.sin(a - half * 1.5)),
            (cx + r_out * math.cos(a - half), cy + r_out * math.sin(a - half)),
            (cx + r_out * math.cos(a + half), cy + r_out * math.sin(a + half)),
            (cx + (r_in + 0.6) * math.cos(a + half * 1.5), cy + (r_in + 0.6) * math.sin(a + half * 1.5)),
        ]
        if i == 0:
            p.move(*pts[0])
        else:
            p.line(*pts[0])
        for q in pts[1:]:
            p.line(*q)
    p.close()
    # alésage central
    p.move(cx + 2.7, cy).arc(cx, cy, 2.7, 0, 360).close()
    return p


# ---------------------------------------------------------------------------
# Définition des icônes
# ---------------------------------------------------------------------------
ICONS: list[Icon] = []


def icon(name, stroke=None, fill=None, mirror=False):
    ICONS.append(Icon(name, stroke, fill, mirror))


# Navigation ---------------------------------------------------------------
p = S()
p.move(4.4, 10.6).line(12, 4.0).line(19.6, 10.6)
p.move(6.5, 9.9).line(6.5, 19.7).line(17.5, 19.7).line(17.5, 9.9)
p.move(10.1, 19.7).line(10.1, 14.4).line(13.9, 14.4).line(13.9, 19.7)
icon("ic_home", stroke=[p])

p = S()
for y in (6.6, 12.0, 17.4):
    p.circle(5.0, y, 1.25)
    p.move(8.6, y).line(19.6, y)
icon("ic_library", stroke=[p], fill=[])

p = S()
p.move(8.4, 6.4).line(19.4, 6.4)
p.move(8.4, 11.4).line(19.4, 11.4)
p.move(8.4, 16.4).line(13.6, 16.4)
p.move(5.0, 6.4).line(5.0, 6.4)
p.move(5.0, 11.4).line(5.0, 11.4)
p.move(5.0, 16.4).line(5.0, 16.4)
f = S()
f.move(15.4, 14.2).line(20.4, 17.0).line(15.4, 19.8).close()
icon("ic_playlist", stroke=[p], fill=[f])

p = S()
p.rounded_rect(4.5, 4.5, 19.5, 19.5, 3.2)
p.circle(12, 12, 2.4)
p.circle(12, 12, 6.2)
icon("ic_album", stroke=[p])

# Lecture ------------------------------------------------------------------
f = S()
f.move(8.0, 4.9).line(19.4, 12.0).line(8.0, 19.1).close()
icon("ic_play", fill=[f])

f = S()
f.rounded_rect(6.6, 5.0, 10.2, 19.0, 1.5)
f.rounded_rect(13.8, 5.0, 17.4, 19.0, 1.5)
icon("ic_pause", fill=[f])

f = S()
f.move(6.0, 5.2).line(14.6, 12.0).line(6.0, 18.8).close()
f.rounded_rect(16.0, 5.2, 18.6, 18.8, 1.3)
icon("ic_next", fill=[f], mirror=True)

f = S()
f.move(18.0, 5.2).line(9.4, 12.0).line(18.0, 18.8).close()
f.rounded_rect(5.4, 5.2, 8.0, 18.8, 1.3)
icon("ic_previous", fill=[f], mirror=True)

f = S()

# Modes de lecture ---------------------------------------------------------
p = S()
p.move(3.6, 7.4).line(6.6, 7.4).cubic(8.4, 7.4, 9.8, 8.3, 11.0, 9.7)
p.cubic(12.3, 11.2, 13.6, 12.9, 15.2, 13.8).cubic(16.0, 14.3, 17.0, 14.6, 18.2, 14.6)
p.line(20.4, 14.6)
p.move(3.6, 14.6).line(6.6, 14.6).cubic(8.4, 14.6, 9.8, 13.7, 11.0, 12.3)
p.cubic(12.3, 10.8, 13.6, 9.1, 15.2, 8.2).cubic(16.0, 7.7, 17.0, 7.4, 18.2, 7.4)
p.line(20.4, 7.4)
arrow_head(p, (20.4, 7.4), "right", 2.6)
arrow_head(p, (20.4, 14.6), "right", 2.6)
icon("ic_shuffle", stroke=[p])

p = S()
p.move(7.4, 5.6).line(16.2, 5.6).cubic(18.4, 5.6, 20.2, 7.4, 20.2, 9.6)
p.line(20.2, 10.8)
arrow_head(p, (20.2, 11.8), "down", 2.6)
p.move(16.6, 18.4).line(7.8, 18.4).cubic(5.6, 18.4, 3.8, 16.6, 3.8, 14.4)
p.line(3.8, 13.2)
arrow_head(p, (3.8, 12.2), "up", 2.6)
icon("ic_repeat", stroke=[p])

p = S()
p.move(7.4, 5.6).line(16.2, 5.6).cubic(18.4, 5.6, 20.2, 7.4, 20.2, 9.6)
p.line(20.2, 10.8)
arrow_head(p, (20.2, 11.8), "down", 2.6)
p.move(16.6, 18.4).line(7.8, 18.4).cubic(5.6, 18.4, 3.8, 16.6, 3.8, 14.4)
p.line(3.8, 13.2)
arrow_head(p, (3.8, 12.2), "up", 2.6)
p.move(10.9, 10.1).line(12.5, 9.0).line(12.5, 15.2)
icon("ic_repeat_one", stroke=[p])

def seek_icon(name: str, clockwise: bool):
    """Anneau ouvert, pointe de flèche tangente et « 10 » lisible à l'intérieur."""
    cx, cy, r = 12.0, 13.0, 8.5
    start = -45.0 if clockwise else 45.0
    sweep = 312.0 if clockwise else -312.0
    end = start + sweep

    ring = S()
    ring.move(cx + r * math.cos(math.radians(start)), cy + r * math.sin(math.radians(start)))
    ring.arc(cx, cy, r, start, end)

    # Pointe de flèche : deux branches opposées au sens de parcours.
    tip = (cx + r * math.cos(math.radians(end)), cy + r * math.sin(math.radians(end)))
    sgn = 1.0 if clockwise else -1.0
    tx, ty = -math.sin(math.radians(end)) * sgn, math.cos(math.radians(end)) * sgn
    size, spread = 3.0, math.radians(38)
    legs = []
    for ang in (spread, -spread):
        bx = -(tx * math.cos(ang) - ty * math.sin(ang))
        by = -(tx * math.sin(ang) + ty * math.cos(ang))
        legs.append((tip[0] + bx * size, tip[1] + by * size))
    ring.move(*legs[0]).line(*tip).line(*legs[1])

    digits = S()
    digits.move(8.6, 11.7).line(9.7, 10.9).line(9.7, 16.4)
    digits.rounded_rect(11.9, 10.9, 15.3, 16.4, 1.7)
    icon(name, stroke=[ring, digits])


seek_icon("ic_forward_10", clockwise=True)
seek_icon("ic_replay_10", clockwise=False)

# Bibliothèque -------------------------------------------------------------
p = S()
p.circle(10.3, 10.3, 6.0)
p.move(14.8, 14.8).line(20.0, 20.0)
icon("ic_search", stroke=[p])

p = S()
p.move(4.2, 7.2).line(13.0, 7.2)
p.move(4.2, 12.0).line(10.8, 12.0)
p.move(4.2, 16.8).line(8.6, 16.8)
p.move(18.4, 6.0).line(18.4, 18.0)
arrow_head(p, (18.4, 6.0), "up", 2.6)
arrow_head(p, (18.4, 18.0), "down", 2.6)
icon("ic_sort", stroke=[p])

p = S()
p.move(4.6, 7.4).line(19.4, 7.4)
p.move(4.6, 12.0).line(19.4, 12.0)
p.move(4.6, 16.6).line(19.4, 16.6)
f = S()
f.circle(9.0, 7.4, 1.9)
f.circle(15.0, 12.0, 1.9)
f.circle(7.6, 16.6, 1.9)
icon("ic_tune", stroke=[p], fill=[f])

p = S()
p.move(3.9, 12.0).line(20.1, 12.0)
p.move(3.9, 6.4).line(20.1, 6.4)
p.move(3.9, 17.6).line(13.4, 17.6)
f = S()
f.move(16.2, 14.9).line(21.0, 17.6).line(16.2, 20.3).close()
icon("ic_queue", stroke=[p], fill=[f])

p = S()
p.move(12.0, 20.2).cubic(12.0, 20.2, 3.4, 14.9, 3.4, 9.2)
p.cubic(3.4, 6.4, 5.7, 4.3, 8.3, 4.3)
p.cubic(10.1, 4.3, 11.4, 5.3, 12.0, 6.3)
p.cubic(12.6, 5.3, 13.9, 4.3, 15.7, 4.3)
p.cubic(18.3, 4.3, 20.6, 6.4, 20.6, 9.2)
p.cubic(20.6, 14.9, 12.0, 20.2, 12.0, 20.2)
p.close()
icon("ic_heart", stroke=[p])

f = S()
f.move(12.0, 20.4).cubic(12.0, 20.4, 3.2, 15.0, 3.2, 9.3)
f.cubic(3.2, 6.4, 5.6, 4.2, 8.3, 4.2)
f.cubic(10.1, 4.2, 11.4, 5.2, 12.0, 6.2)
f.cubic(12.6, 5.2, 13.9, 4.2, 15.7, 4.2)
f.cubic(18.4, 4.2, 20.8, 6.4, 20.8, 9.3)
f.cubic(20.8, 15.0, 12.0, 20.4, 12.0, 20.4)
f.close()
icon("ic_heart_filled", fill=[f])

p = S()
p.move(12.0, 19.6).arc(12.0, 12.0, 7.6, 0, 360)
p.move(12.0, 7.4).line(12.0, 12.2).line(15.6, 14.2)
icon("ic_history", stroke=[p])

p = S()
p.move(12.0, 3.6).line(19.4, 6.6).line(19.4, 12.2)
p.cubic(19.4, 16.4, 16.4, 19.7, 12.0, 21.0)
p.cubic(7.6, 19.7, 4.6, 16.4, 4.6, 12.2)
p.line(4.6, 6.6).close()
p.move(8.6, 12.0).line(11.2, 14.6).line(15.8, 9.6)
icon("ic_shield", stroke=[p])

p = S()
p.move(4.0, 9.0).cubic(4.0, 8.0, 4.8, 7.2, 5.8, 7.2)
p.line(9.2, 7.2).line(11.4, 9.6).line(18.2, 9.6)
p.cubic(19.2, 9.6, 20.0, 10.4, 20.0, 11.4)
p.line(20.0, 17.8).cubic(20.0, 18.8, 19.2, 19.6, 18.2, 19.6)
p.line(5.8, 19.6).cubic(4.8, 19.6, 4.0, 18.8, 4.0, 17.8).close()
icon("ic_folder", stroke=[p])

# Utilitaires --------------------------------------------------------------
p = S()
p.move(6.4, 6.4).line(17.6, 17.6)
p.move(17.6, 6.4).line(6.4, 17.6)
icon("ic_close", stroke=[p])

p = S()
p.move(7.0, 14.6).line(12.0, 9.6).line(17.0, 14.6)
icon("ic_chevron_up", stroke=[p])

p = S()
p.move(7.0, 9.4).line(12.0, 14.4).line(17.0, 9.4)
icon("ic_chevron_down", stroke=[p])

p = S()
p.move(9.4, 7.0).line(14.4, 12.0).line(9.4, 17.0)
icon("ic_chevron_right", stroke=[p], mirror=True)

p = S()
p.move(19.4, 12.0).line(5.2, 12.0)
arrow_head(p, (4.6, 12.0), "left", 3.0)
icon("ic_arrow_back", stroke=[p], mirror=True)

p = S()
p.move(12.0, 5.0).line(12.0, 19.0)
p.move(5.0, 12.0).line(19.0, 12.0)
icon("ic_add", stroke=[p])

p = S()
p.circle(12.0, 5.4, 1.7)
p.circle(12.0, 12.0, 1.7)
p.circle(12.0, 18.6, 1.7)
icon("ic_more", fill=[S().circle(12.0, 5.4, 1.7), S().circle(12.0, 12.0, 1.7),
                      S().circle(12.0, 18.6, 1.7)])

p = S()
p.move(19.4, 8.2).arc(12.0, 12.6, 7.6, -32, -300)
arrow_head(p, (12.6, 5.0), "left", 2.8)
icon("ic_refresh", stroke=[p])

p = S()
p.move(5.6, 12.4).line(9.8, 16.6).line(18.4, 7.6)
icon("ic_check", stroke=[p])

p = S()
p.move(4.8, 7.0).line(19.2, 7.0)
p.move(9.6, 7.0).line(9.6, 4.6).line(14.4, 4.6).line(14.4, 7.0)
p.move(6.6, 7.0).line(7.5, 18.6).cubic(7.6, 19.5, 8.3, 20.2, 9.2, 20.2)
p.line(14.8, 20.2).cubic(15.7, 20.2, 16.4, 19.5, 16.5, 18.6).line(17.4, 7.0)
p.move(10.6, 10.8).line(10.9, 16.4)
p.move(13.4, 10.8).line(13.1, 16.4)
icon("ic_delete", stroke=[p])

p = S()
p.move(4.6, 19.4).line(5.4, 15.0).line(15.6, 4.8)
p.cubic(16.4, 4.0, 17.6, 4.0, 18.4, 4.8).cubic(19.2, 5.6, 19.2, 6.8, 18.4, 7.6)
p.line(8.2, 18.0).line(4.6, 19.4).close()
p.move(13.6, 6.8).line(16.6, 9.8)
icon("ic_edit", stroke=[p])

p = S()

p = S()
p.move(8.2, 17.0).circle(8.2, 17.0, 2.4)
p.move(10.6, 17.0).line(10.6, 6.3).line(18.4, 4.4).line(18.4, 14.6)
p.circle(15.8, 14.6, 2.4)
icon("ic_note", stroke=[p])

p = S()
p.circle(12.0, 12.0, 8.2)
p.move(12.0, 8.0).line(12.0, 12.4)
p.move(12.0, 15.9).line(12.0, 15.9)
icon("ic_info", stroke=[p])

p = S()
p.move(12.0, 3.2).line(20.6, 18.4).line(3.4, 18.4).close()
p.move(12.0, 9.4).line(12.0, 13.6)
p.move(12.0, 16.2).line(12.0, 16.2)
icon("ic_warning", stroke=[p])

p = S()
p.rounded_rect(4.6, 10.2, 19.4, 19.4, 2.6)
p.move(8.2, 10.2).line(8.2, 7.4).cubic(8.2, 5.3, 9.9, 3.6, 12.0, 3.6)
p.cubic(14.1, 3.6, 15.8, 5.3, 15.8, 7.4).line(15.8, 10.2)
icon("ic_lock", stroke=[p])

p = S()
p.move(12.0, 4.0).line(12.0, 14.4)
arrow_head(p, (12.0, 15.6), "down", 2.8)
p.move(4.6, 16.6).line(4.6, 18.6).cubic(4.6, 19.4, 5.2, 20.0, 6.0, 20.0)
p.line(18.0, 20.0).cubic(18.8, 20.0, 19.4, 19.4, 19.4, 18.6).line(19.4, 16.6)
icon("ic_download", stroke=[p])


# ---------------------------------------------------------------------------
# Écriture des fichiers
# ---------------------------------------------------------------------------
def write_xmls():
    OUT.mkdir(parents=True, exist_ok=True)
    for ic in ICONS:
        (OUT / f"{ic.name}.xml").write_text(ic.xml(), encoding="utf-8")
    print(f"{len(ICONS)} icônes vectorielles écrites dans app/src/main/res/drawable/")


def write_preview():
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        print("Pillow absent : aperçu non généré.")
        return

    SS = 8  # suréchantillonnage
    cell = 34  # 24 dp + marge
    cols = 8
    rows = math.ceil(len(ICONS) / cols)
    scale = 4  # agrandissement d'affichage

    def render(ic: Icon, target: int, color) -> Image.Image:
        big = target * SS
        img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        k = big / GRID
        for path in ic.fill_paths:
            for poly in path.sample(0.2):
                d.polygon([(x * k, y * k) for x, y in poly], fill=color)
        for path in ic.stroke_paths:
            for poly in path.sample(0.2):
                pts = [(x * k, y * k) for x, y in poly]
                w = STROKE * k
                d.line(pts, fill=color, width=int(round(w)), joint="curve")
                r = w / 2
                for (x, y) in (pts[0], pts[-1]):
                    d.ellipse([x - r, y - r, x + r, y + r], fill=color)
        return img.resize((target, target), Image.LANCZOS)

    small_px = 24 * scale // 2  # 48 px = 24 dp sur écran xhdpi, rendu net
    W = cols * cell * scale + 40
    H = rows * (cell * scale + 18) + 60
    sheet = Image.new("RGB", (W, H), (255, 255, 255))
    d = ImageDraw.Draw(sheet)
    d.text((20, 16), "Pylive Music — icônes (rendu 48 px et 24 px réels)", fill=(26, 16, 24))
    for i, ic in enumerate(ICONS):
        cx = 20 + (i % cols) * cell * scale
        cy = 50 + (i // cols) * (cell * scale + 18)
        big_icon = render(ic, 96, (184, 13, 184, 255))
        sheet.paste(big_icon, (cx, cy), big_icon)
        tiny = render(ic, 24, (26, 16, 24, 255))
        sheet.paste(tiny, (cx + 96 + 6, cy + 36), tiny)
        d.text((cx, cy + 104), ic.name.replace("ic_", ""), fill=(92, 78, 94))
    sheet.save(DOCS / "icones-systeme.png")
    print("aperçu écrit dans docs/icones-systeme.png")


if __name__ == "__main__":
    write_xmls()
    write_preview()
