"""Writes assets-raw/explainers/e29-2.svg: the speed a wing needs to hold its height, against orbital speed.

The curve is real air (US Standard Atmosphere 1976 densities, interpolated in log) and one imagined plane whose
wing loading puts the crossing at von Karman's 84 km: v = sqrt(K / rho). It also prints where a craft with a
tenth to ten times that wing loading would cross (the 70 to 90 km band).

Run from the repo root, then make_explainer_art.py:
    python assets-raw/explainers/make_e29_2.py assets-raw/explainers/e29-1.svg assets-raw/explainers/e29-2.svg
(e29-1.svg lends its styles and patterns.)
"""
import math
import sys

DENSITY = [(0, 1.225), (10, 0.4135), (20, 0.08891), (30, 0.01841), (40, 0.003996), (50, 0.001027),
           (60, 3.097e-4), (70, 8.283e-5), (80, 1.846e-5), (90, 3.416e-6), (100, 5.604e-7), (110, 9.7e-8)]


def rho(h):
    for (h0, r0), (h1, r1) in zip(DENSITY, DENSITY[1:]):
        if h0 <= h <= h1:
            f = (h - h0) / (h1 - h0)
            return math.exp(math.log(r0) + f * (math.log(r1) - math.log(r0)))
    raise ValueError(h)


ORBIT = 7.8
CROSS = 84.0
K = ORBIT ** 2 * rho(CROSS)

X0, X1, VMAX = 110, 1000, 10.0
Y0, PX_KM = 590, 4.6


def x(v):
    return X0 + (X1 - X0) * v / VMAX


def y(h):
    return Y0 - PX_KM * h


def needed(h):
    return math.sqrt(K / rho(h))


N = '#16305c'
G = '#e0a72e'
LINE = 'fill="none" stroke-linejoin="round" stroke-linecap="round"'
out = []
add = out.append
add('<text x="24" y="44" class="d-head">How fast a wing must fly to hold its height</text>')
# the band where real craft cross
add(f'<rect x="{X0}" y="{y(90):.1f}" width="{X1 + 40 - X0}" height="{y(70) - y(90):.1f}" fill="url(#hatch)" opacity="0.6"/>')
add(f'<text x="{X0 + 10}" y="{y(70) - 8:.1f}" class="d-sm">Real craft: 70 to 90 km</text>')
# axes
add(f'<path d="M{X0} {Y0} L{X1 + 40} {Y0}" stroke="{N}" stroke-width="2" {LINE}/>')
add(f'<path d="M{X0} {Y0} L{X0} {y(110):.1f}" stroke="{N}" stroke-width="1.5" {LINE}/>')
for h in range(0, 101, 20):
    add(f'<path d="M{X0 - 6} {y(h):.1f} L{X0} {y(h):.1f}" stroke="{N}" stroke-width="1.5" {LINE}/>')
    add(f'<text x="{X0 - 12}" y="{y(h) + 4:.1f}" text-anchor="end" class="d-sm">{h} km</text>')
for v in range(0, 11, 2):
    add(f'<path d="M{x(v):.1f} {Y0} L{x(v):.1f} {Y0 + 6}" stroke="{N}" stroke-width="1.5" {LINE}/>')
    label = f'{v} km/s' if v == 10 else str(v)
    add(f'<text x="{x(v):.1f}" y="{Y0 + 20}" text-anchor="middle" class="d-sm">{label}</text>')
# the round numbers
add(f'<path d="M{X0} {y(100):.1f} L{X1 + 40} {y(100):.1f}" stroke="{G}" stroke-width="1.5" stroke-dasharray="6 5" {LINE}/>')
add(f'<text x="{X0 + 10}" y="{y(100) - 8:.1f}" class="d-lbl-gold">100 km: the round number most people use</text>')
add(f'<path d="M{X0} {y(80.47):.1f} L{x(ORBIT) - 14:.1f} {y(80.47):.1f}" stroke="{N}" stroke-width="1" stroke-dasharray="2 5" {LINE}/>')
add(f'<text x="{X0 + 10}" y="{y(80.47) - 8:.1f}" class="d-sm">50 miles, about 80 km: the US line</text>')
# orbital speed
add(f'<path d="M{x(ORBIT):.1f} {Y0} L{x(ORBIT):.1f} {y(110):.1f}" stroke="{N}" stroke-width="2" stroke-dasharray="8 5" {LINE}/>')
add(f'<text x="{x(ORBIT) + 10:.1f}" y="{y(30):.1f}" class="d-lbl">Orbital speed</text>')
add(f'<text x="{x(ORBIT) + 10:.1f}" y="{y(30) + 16:.1f}" class="d-txt">7.8 km/s, about 28,000 km/h</text>')
# the curve
points = []
h = 0.0
while True:
    v = needed(h)
    if v > VMAX:
        break
    points.append((x(v), y(h)))
    h += 0.25
d = 'M' + ' L'.join(f'{px:.1f} {py:.1f}' for px, py in points)
add(f'<path d="{d}" stroke="{N}" stroke-width="3" {LINE}/>')
add(f'<text x="{x(needed(56)) + 14:.1f}" y="{y(56) + 4:.1f}" class="d-lbl">The speed a wing needs</text>')
add(f'<text x="{x(needed(56)) + 14:.1f}" y="{y(56) + 20:.1f}" class="d-txt">Thinner air, faster flight</text>')
# where they meet
cx, cy = x(ORBIT), y(CROSS)
add(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="7" fill="{G}" stroke="{N}" stroke-width="2"/>')
add(f'<text x="{cx - 14:.1f}" y="{cy - 30:.1f}" text-anchor="end" class="d-lbl-gold halo">Von Kármán: about 84 km</text>')
add(f'<text x="{cx - 14:.1f}" y="{cy - 14:.1f}" text-anchor="end" class="d-txt halo">Above this, flying fast enough means orbiting</text>')
# the two regions
add(f'<text x="{x(1.2):.1f}" y="{y(12):.1f}" class="d-lbl">Flight</text>')
add(f'<text x="{x(1.2):.1f}" y="{y(12) + 16:.1f}" class="d-txt">The wings hold the plane up</text>')
add(f'<text x="{X1 + 36}" y="{y(60):.1f}" text-anchor="end" class="d-lbl">Orbit</text>')
add(f'<text x="{X1 + 36}" y="{y(60) + 16:.1f}" text-anchor="end" class="d-txt">Falling around the world,</text>')
add(f'<text x="{X1 + 36}" y="{y(60) + 32:.1f}" text-anchor="end" class="d-txt">wings or not</text>')
add('<text x="24" y="640" class="d-sm">Heights to scale · the curve: one imagined plane in real air</text>')

head = open(sys.argv[1], encoding='utf-8').read()
head = head[: head.index('</defs>') + len('</defs>')]
svg = head + '\n          ' + ''.join(out) + '\n        \n</svg>'
open(sys.argv[2], 'w', encoding='utf-8', newline='\n').write(svg)
print('K', K, 'ground', needed(0) * 1000, 'm/s', '11 km', needed(11) * 1000, '50 km', needed(50), 'heavier x10 crossing:')
for factor in (0.3, 3, 10):
    lo, hi = 0.0, 109.0
    for _ in range(60):
        mid = (lo + hi) / 2
        if math.sqrt(K * factor / rho(mid)) > ORBIT:
            hi = mid
        else:
            lo = mid
    print(' wing loading x', factor, round(lo, 1), 'km')
