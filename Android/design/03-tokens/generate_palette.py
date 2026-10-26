#!/usr/bin/env python3
"""Generate DropShot tonal palettes, M3 color roles, contrast table, JSON tokens and Color.kt.

Tone = CIE L* (identical to the M3 HCT tone axis). Hue and chroma are held in
CIE LCh(ab) and chroma is reduced per tone until the color fits in sRGB.
This approximates HCT; the tone values (and so the contrast) are exact.
"""
import json, math, sys, os

OUT = sys.argv[1] if len(sys.argv) > 1 else "."

# ---------- color math ----------
WX, WY, WZ = 0.95047, 1.0, 1.08883

def srgb_to_lin(c):
    c /= 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4

def lin_to_srgb(c):
    v = 12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055
    return v * 255.0

def hex_to_rgb(h):
    h = h.lstrip('#'); return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))

def rgb_to_hex(r):
    return '#%02X%02X%02X' % tuple(r)

def f(t):
    return t ** (1/3) if t > (6/29) ** 3 else t / (3 * (6/29) ** 2) + 4/29

def finv(t):
    return t ** 3 if t > 6/29 else 3 * (6/29) ** 2 * (t - 4/29)

def rgb_to_lch(rgb):
    r, g, b = (srgb_to_lin(c) for c in rgb)
    x = 0.4124564*r + 0.3575761*g + 0.1804375*b
    y = 0.2126729*r + 0.7151522*g + 0.0721750*b
    z = 0.0193339*r + 0.1191920*g + 0.9503041*b
    fx, fy, fz = f(x/WX), f(y/WY), f(z/WZ)
    L = 116*fy - 16; a = 500*(fx-fy); bb = 200*(fy-fz)
    return L, math.hypot(a, bb), math.degrees(math.atan2(bb, a)) % 360

def lch_to_lin(L, C, H):
    a = C*math.cos(math.radians(H)); b = C*math.sin(math.radians(H))
    fy = (L+16)/116; fx = fy + a/500; fz = fy - b/200
    x, y, z = WX*finv(fx), WY*finv(fy), WZ*finv(fz)
    r = 3.2404542*x - 1.5371385*y - 0.4985314*z
    g = -0.9692660*x + 1.8760108*y + 0.0415560*z
    bl = 0.0556434*x - 0.2040259*y + 1.0572252*z
    return r, g, bl

def in_gamut(lin, eps=1e-6):
    return all(-eps <= c <= 1+eps for c in lin)

def tone_color(L, C, H):
    if L <= 0: return '#000000'
    if L >= 100: return '#FFFFFF'
    lo, hi = 0.0, C
    if not in_gamut(lch_to_lin(L, C, H)):
        for _ in range(40):
            mid = (lo+hi)/2
            if in_gamut(lch_to_lin(L, mid, H)): lo = mid
            else: hi = mid
        C = lo
    lin = lch_to_lin(L, C, H)
    rgb = [min(255, max(0, round(lin_to_srgb(min(1, max(0, c)))))) for c in lin]
    return rgb_to_hex(rgb)

def luminance(hexv):
    r, g, b = (srgb_to_lin(c) for c in hex_to_rgb(hexv))
    return 0.2126*r + 0.7152*g + 0.0722*b

def contrast(a, b):
    la, lb = luminance(a), luminance(b)
    if la < lb: la, lb = lb, la
    return (la+0.05)/(lb+0.05)

# ---------- palettes ----------
TONES = [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100]
EXTRA_NEUTRAL = [4, 6, 12, 17, 22, 24, 87, 92, 94, 96, 98]

def seed_hue(hexv):
    return rgb_to_lch(hex_to_rgb(hexv))[2]

PAL_DEF = {
    # name: (seed hex, chroma in LCh units, note)
    'primary':        ('#D9501E', 72, 'Brand orange from the DropShot logo ball'),
    'secondary':      ('#7A5B45', 18, 'Warm clay brown, near the logo outline hue'),
    'tertiary':       ('#0F7C86', 28, 'Court teal, cool contrast accent'),
    'neutral':        ('#FBF5EA', 4,  'Warm cream from the logo background'),
    'neutralVariant': ('#FBF5EA', 9,  'Warm sand'),
    'error':          ('#B3261E', 80, 'M3 baseline error red'),
    'success':        ('#2E7D32', 50, 'Green'),
    'warning':        ('#B26B00', 62, 'Amber'),
    'info':           ('#1F63C6', 55, 'Blue'),
    'playerBlue':     ('#3184FF', 70, 'Existing app color BLUE'),
    'playerRed':      ('#F45C5C', 70, 'Existing app color RED'),
    'playerGreen':    ('#39B87F', 55, 'Existing app color GREEN'),
    'playerGold':     ('#FFC857', 70, 'Existing app color GOLD'),
}

palettes = {}
for name, (seed, chroma, _) in PAL_DEF.items():
    h = seed_hue(seed)
    tones = TONES + (EXTRA_NEUTRAL if name == 'neutral' else [])
    palettes[name] = {t: tone_color(t, chroma, h) for t in sorted(tones)}

class Ref(str):
    pass

def P(pal, t):
    r = Ref(palettes[pal][t]); r.pal = pal; r.tone = t; return r

def kname(r):
    return r.pal[0].upper() + r.pal[1:] + str(r.tone)

# ---------- roles ----------
def scheme(dark=False, hc=False):
    p, s, t, n, v, e = 'primary', 'secondary', 'tertiary', 'neutral', 'neutralVariant', 'error'
    if not dark and not hc:
        acc = lambda pal: {'': P(pal,40), 'on': P(pal,100), 'container': P(pal,90), 'onContainer': P(pal,10)}
    elif dark and not hc:
        acc = lambda pal: {'': P(pal,80), 'on': P(pal,20), 'container': P(pal,30), 'onContainer': P(pal,90)}
    elif not dark and hc:
        acc = lambda pal: {'': P(pal,20), 'on': P(pal,100), 'container': P(pal,30), 'onContainer': P(pal,100)}
    else:
        acc = lambda pal: {'': P(pal,90), 'on': P(pal,0), 'container': P(pal,80), 'onContainer': P(pal,0)}
    r = {}
    for role, pal in (('primary', p), ('secondary', s), ('tertiary', t), ('error', e)):
        a = acc(pal)
        r[role] = a['']
        r['on' + role.capitalize()] = a['on']
        r[role + 'Container'] = a['container']
        r['on' + role.capitalize() + 'Container'] = a['onContainer']
    if not dark:
        r.update({
            'background': P(n,98), 'onBackground': P(n,0 if hc else 10),
            'surface': P(n,98), 'onSurface': P(n,0 if hc else 10),
            'surfaceVariant': P(v,90), 'onSurfaceVariant': P(v,10 if hc else 30),
            'outline': P(v,20 if hc else 50), 'outlineVariant': P(v,30 if hc else 80),
            'inverseSurface': P(n,20), 'inverseOnSurface': P(n,95 if not hc else 100),
            'inversePrimary': P(p,80 if not hc else 90),
            'surfaceDim': P(n,87), 'surfaceBright': P(n,98),
            'surfaceContainerLowest': P(n,100), 'surfaceContainerLow': P(n,96),
            'surfaceContainer': P(n,94), 'surfaceContainerHigh': P(n,92),
            'surfaceContainerHighest': P(n,90),
        })
    else:
        r.update({
            'background': P(n,6), 'onBackground': P(n,100 if hc else 90),
            'surface': P(n,6), 'onSurface': P(n,100 if hc else 90),
            'surfaceVariant': P(v,30), 'onSurfaceVariant': P(v,95 if hc else 80),
            'outline': P(v,90 if hc else 60), 'outlineVariant': P(v,80 if hc else 30),
            'inverseSurface': P(n,90), 'inverseOnSurface': P(n,20 if not hc else 0),
            'inversePrimary': P(p,40 if not hc else 30),
            'surfaceDim': P(n,6), 'surfaceBright': P(n,24),
            'surfaceContainerLowest': P(n,4), 'surfaceContainerLow': P(n,10),
            'surfaceContainer': P(n,12), 'surfaceContainerHigh': P(n,17),
            'surfaceContainerHighest': P(n,22),
        })
    r['surfaceTint'] = r['primary']
    r['scrim'] = P(n,0); r['shadow'] = P(n,0)
    return r

def semantic(dark=False, hc=False):
    out = {}
    for pal in ('success', 'warning', 'info', 'error'):
        if not dark and not hc: tt = (40, 100, 90, 10)
        elif dark and not hc: tt = (80, 20, 30, 90)
        elif not dark: tt = (20, 100, 30, 100)
        else: tt = (90, 0, 80, 0)
        out[pal] = {'color': P(pal,tt[0]), 'onColor': P(pal,tt[1]), 'container': P(pal,tt[2]), 'onContainer': P(pal,tt[3])}
    return out

def players(dark=False, hc=False):
    out = {}
    for pal, label in (('playerBlue','Blue'),('playerRed','Red'),('playerGreen','Green'),('playerGold','Gold')):
        fill = 30 if hc else 40
        acc = (90 if dark else 30) if hc else (80 if dark else 40)
        out[label] = {'fill': P(pal,fill), 'onFill': P(pal,100), 'accentOnSurface': P(pal, acc)}
    return out

schemes = {
    'light': scheme(False), 'dark': scheme(True),
    'lightHighContrast': scheme(False, True), 'darkHighContrast': scheme(True, True),
}
sem = {k: semantic(*a) for k, a in (('light',(False,False)),('dark',(True,False)),('lightHighContrast',(False,True)),('darkHighContrast',(True,True)))}
ply = {k: players(*a) for k, a in (('light',(False,False)),('dark',(True,False)),('lightHighContrast',(False,True)),('darkHighContrast',(True,True)))}

# ---------- contrast table ----------
PAIRS = [
    # fg, bg, kind  (kind: 'text' needs 4.5, 'large' / 'ui' needs 3.0)
    ('onPrimary','primary','text'), ('onPrimaryContainer','primaryContainer','text'),
    ('onSecondary','secondary','text'), ('onSecondaryContainer','secondaryContainer','text'),
    ('onTertiary','tertiary','text'), ('onTertiaryContainer','tertiaryContainer','text'),
    ('onError','error','text'), ('onErrorContainer','errorContainer','text'),
    ('onBackground','background','text'), ('onSurface','surface','text'),
    ('onSurface','surfaceContainerLowest','text'), ('onSurface','surfaceContainerLow','text'),
    ('onSurface','surfaceContainer','text'), ('onSurface','surfaceContainerHigh','text'),
    ('onSurface','surfaceContainerHighest','text'),
    ('onSurfaceVariant','surface','text'), ('onSurfaceVariant','surfaceContainer','text'),
    ('onSurfaceVariant','surfaceContainerHighest','text'), ('onSurfaceVariant','surfaceVariant','text'),
    ('primary','surface','text'), ('primary','surfaceContainer','text'),
    ('secondary','surface','text'), ('tertiary','surface','text'), ('error','surface','text'),
    ('inverseOnSurface','inverseSurface','text'), ('inversePrimary','inverseSurface','text'),
    ('outline','surface','ui'), ('outline','surfaceContainerHighest','ui'),
    ('primary','surfaceContainer','ui'), ('secondary','surface','ui'),
]

def contrast_rows(name):
    s = schemes[name]; rows = []
    for fg, bg, kind in PAIRS:
        c = contrast(s[fg], s[bg]); need = 4.5 if kind == 'text' else 3.0
        rows.append((fg, s[fg], bg, s[bg], kind, c, need, c >= need))
    se = sem[name]
    for k, v in se.items():
        for fg, bg in (('onColor','color'),('onContainer','container')):
            c = contrast(v[fg], v[bg]); rows.append((f'{k}.{fg}', v[fg], f'{k}.{bg}', v[bg], 'text', c, 4.5, c >= 4.5))
        c = contrast(v['color'], s['surface']); rows.append((f'{k}.color', v['color'], 'surface', s['surface'], 'text', c, 4.5, c >= 4.5))
    for k, v in ply[name].items():
        c = contrast(v['onFill'], v['fill']); rows.append((f'player{k}.onFill', v['onFill'], f'player{k}.fill', v['fill'], 'text', c, 4.5, c >= 4.5))
        c = contrast(v['accentOnSurface'], s['surface']); rows.append((f'player{k}.accent', v['accentOnSurface'], 'surface', s['surface'], 'text', c, 4.5, c >= 4.5))
    return rows

# ---------- outputs ----------
md = []
md.append('### Tonal palettes\n')
md.append('| Palette | Seed | ' + ' | '.join(f'T{t}' for t in TONES) + ' |')
md.append('|---|---|' + '---|' * len(TONES))
for name, (seed, chroma, note) in PAL_DEF.items():
    md.append(f'| {name} | `{seed}` | ' + ' | '.join(f'`{palettes[name][t]}`' for t in TONES) + ' |')
md.append('\nAdditional neutral tones for the M3 surface roles: ' + ', '.join(f'N{t} `{palettes["neutral"][t]}`' for t in EXTRA_NEUTRAL) + '.\n')

for name in schemes:
    md.append(f'\n### Color roles: {name}\n')
    md.append('| Role | HEX |\n|---|---|')
    for k, v in schemes[name].items(): md.append(f'| {k} | `{v}` |')

md.append('\n### Semantic colors\n')
md.append('| Role | Light | Dark | Light high contrast | Dark high contrast |\n|---|---|---|---|---|')
for k in ('success','warning','error','info'):
    for r in ('color','onColor','container','onContainer'):
        md.append(f'| {k}.{r} | ' + ' | '.join(f'`{sem[s][k][r]}`' for s in sem) + ' |')

md.append('\n### Player identity colors\n')
md.append('| Player color | Role | Light | Dark | Light high contrast | Dark high contrast |\n|---|---|---|---|---|---|')
for k in ('Blue','Red','Green','Gold'):
    for r in ('fill','onFill','accentOnSurface'):
        md.append(f'| {k} | {r} | ' + ' | '.join(f'`{ply[s][k][r]}`' for s in ply) + ' |')

all_pass = True
for name in schemes:
    md.append(f'\n### Contrast ratios: {name}\n')
    md.append('| Foreground | Background | Use | Ratio | Minimum | Result |\n|---|---|---|---|---|---|')
    for fg, fh, bg, bh, kind, c, need, ok in contrast_rows(name):
        all_pass &= ok
        use = {'text':'Normal text','ui':'UI part / large text'}[kind]
        md.append(f'| {fg} `{fh}` | {bg} `{bh}` | {use} | {c:.2f}:1 | {need}:1 | {"Pass" if ok else "FAIL"} |')

with open(os.path.join(OUT, 'color_tables.md'), 'w') as fh: fh.write('\n'.join(md) + '\n')

tokens = {
    'meta': {'name': 'DropShot', 'version': '1.0.0', 'toneModel': 'CIE L* tone, LCh(ab) hue and chroma, sRGB gamut clip'},
    'palette': {k: {str(t): v for t, v in p.items()} for k, p in palettes.items()},
    'scheme': schemes, 'semantic': sem, 'player': ply,
}


# ---------- Kotlin + full JSON generation ----------
def kt(hexv): return '0xFF' + hexv.lstrip('#')

HDR = ['// Generated from design/03-tokens/design-tokens.json by design/03-tokens/generate_palette.py.',
       '// Tone = CIE L* (the M3 HCT tone axis). Do not edit the HEX values by hand.',
       '// Change the seed values in the generator and run it again.']

c = ['package pl.maciek.squashscoreboard.ui.theme', '', 'import androidx.compose.runtime.Immutable',
     'import androidx.compose.ui.graphics.Color', ''] + HDR + ['']
c.append('// ---------- Tonal palettes (T0 to T100) ----------')
for name, p in palettes.items():
    c.append(f'// {name}: seed {PAL_DEF[name][0]}. {PAL_DEF[name][2]}.')
    for t, v in p.items():
        c.append(f'val {name[0].upper()+name[1:]}{t} = Color({kt(v)})')
    c.append('')

c += [
'// ---------- Extended colors (not part of the M3 ColorScheme) ----------',
'',
'/** A semantic color set. Always show an icon and a text label with it. */',
'@Immutable',
'data class SemanticColor(',
'    val color: Color,',
'    val onColor: Color,',
'    val container: Color,',
'    val onContainer: Color,',
')',
'',
'/** A player identity color. The fill is for the score panel. The accent is for text and chart lines on the surface. */',
'@Immutable',
'data class PlayerColor(',
'    val key: String,',
'    val fill: Color,',
'    val onFill: Color,',
'    val accentOnSurface: Color,',
')',
'',
'@Immutable',
'data class ExtendedColors(',
'    val success: SemanticColor,',
'    val warning: SemanticColor,',
'    val error: SemanticColor,',
'    val info: SemanticColor,',
'    val playerBlue: PlayerColor,',
'    val playerRed: PlayerColor,',
'    val playerGreen: PlayerColor,',
'    val playerGold: PlayerColor,',
') {',
'    val players: List<PlayerColor> get() = listOf(playerBlue, playerRed, playerGreen, playerGold)',
'    fun player(key: String): PlayerColor = players.firstOrNull { it.key == key } ?: playerBlue',
'}',
'',
]
for sname in ('light', 'dark', 'lightHighContrast', 'darkHighContrast'):
    se = sem[sname]; pl = ply[sname]
    def sc(k):
        v = se[k]
        return f'SemanticColor({kname(v["color"])}, {kname(v["onColor"])}, {kname(v["container"])}, {kname(v["onContainer"])})'
    def pc(k, key):
        v = pl[k]
        return f'PlayerColor("{key}", {kname(v["fill"])}, {kname(v["onFill"])}, {kname(v["accentOnSurface"])})'
    vname = sname[0].upper() + sname[1:] + 'ExtendedColors'
    c += [f'val {vname} = ExtendedColors(',
          f'    success = {sc("success")},',
          f'    warning = {sc("warning")},',
          f'    error = {sc("error")},',
          f'    info = {sc("info")},',
          f'    playerBlue = {pc("Blue","blue")},',
          f'    playerRed = {pc("Red","red")},',
          f'    playerGreen = {pc("Green","green")},',
          f'    playerGold = {pc("Gold","gold")},',
          ')', '']
with open(os.path.join(OUT, 'Color.kt'), 'w') as fh: fh.write('\n'.join(c))

ROLE_ORDER = ['primary','onPrimary','primaryContainer','onPrimaryContainer','inversePrimary',
    'secondary','onSecondary','secondaryContainer','onSecondaryContainer',
    'tertiary','onTertiary','tertiaryContainer','onTertiaryContainer',
    'background','onBackground','surface','onSurface','surfaceVariant','onSurfaceVariant','surfaceTint',
    'inverseSurface','inverseOnSurface','error','onError','errorContainer','onErrorContainer',
    'outline','outlineVariant','scrim','surfaceBright','surfaceContainer','surfaceContainerHigh',
    'surfaceContainerHighest','surfaceContainerLow','surfaceContainerLowest','surfaceDim']
t = []
for sname in ('light', 'dark', 'lightHighContrast', 'darkHighContrast'):
    fn = 'darkColorScheme' if sname.startswith('dark') else 'lightColorScheme'
    t.append(f'internal val {sname[0].upper()+sname[1:]}ColorScheme = {fn}(')
    for role in ROLE_ORDER:
        t.append(f'    {role} = {kname(schemes[sname][role])},')
    t.append(')'); t.append('')
with open(os.path.join(OUT, 'theme_schemes_fragment.kt'), 'w') as fh: fh.write('\n'.join(t))

# role -> tone map for the docs
rm = ['| Role | Light | Dark | Light high contrast | Dark high contrast |', '|---|---|---|---|---|']
for role in ROLE_ORDER + ['shadow']:
    rm.append(f'| {role} | ' + ' | '.join(f'{kname(schemes[s][role])} `{schemes[s][role]}`' for s in ('light','dark','lightHighContrast','darkHighContrast')) + ' |')
with open(os.path.join(OUT, 'role_table.md'), 'w') as fh: fh.write('\n'.join(rm) + '\n')

TYPE = {
  'fontFamily': {'brand': 'Roboto (system sans-serif, FontFamily.Default)', 'numerals': 'Roboto with tabular figures (font feature "tnum")'},
  'styles': {
    'displayLarge':   {'size': 57, 'lineHeight': 64, 'weight': 400, 'tracking': -0.25},
    'displayMedium':  {'size': 45, 'lineHeight': 52, 'weight': 400, 'tracking': 0},
    'displaySmall':   {'size': 36, 'lineHeight': 44, 'weight': 400, 'tracking': 0},
    'headlineLarge':  {'size': 32, 'lineHeight': 40, 'weight': 400, 'tracking': 0},
    'headlineMedium': {'size': 28, 'lineHeight': 36, 'weight': 400, 'tracking': 0},
    'headlineSmall':  {'size': 24, 'lineHeight': 32, 'weight': 400, 'tracking': 0},
    'titleLarge':     {'size': 22, 'lineHeight': 28, 'weight': 500, 'tracking': 0},
    'titleMedium':    {'size': 18, 'lineHeight': 26, 'weight': 500, 'tracking': 0.15},
    'titleSmall':     {'size': 16, 'lineHeight': 24, 'weight': 500, 'tracking': 0.1},
    'bodyLarge':      {'size': 18, 'lineHeight': 28, 'weight': 400, 'tracking': 0.15},
    'bodyMedium':     {'size': 16, 'lineHeight': 24, 'weight': 400, 'tracking': 0.25},
    'bodySmall':      {'size': 16, 'lineHeight': 24, 'weight': 400, 'tracking': 0.4},
    'labelLarge':     {'size': 16, 'lineHeight': 24, 'weight': 500, 'tracking': 0.1},
    'labelMedium':    {'size': 14, 'lineHeight': 20, 'weight': 500, 'tracking': 0.5},
    'labelSmall':     {'size': 12, 'lineHeight': 16, 'weight': 500, 'tracking': 0.5},
  },
  'custom': {
    'scoreDisplay': {'size': 'fit to panel, 96 to 240', 'lineHeight': '1.0 x size', 'weight': 700, 'tracking': 0, 'features': 'tnum'},
    'gameScore':    {'size': 32, 'lineHeight': 40, 'weight': 700, 'tracking': 0, 'features': 'tnum'},
    'clock':        {'size': 22, 'lineHeight': 28, 'weight': 500, 'tracking': 0, 'features': 'tnum'},
  },
  'unit': 'sp', 'maxFontScale': 2.0,
}
SPACING = {'unit': 'dp', 'grid': 8, 'halfStep': 4, 'scale': {'s0': 0, 's1': 4, 's2': 8, 's3': 16, 's4': 24, 's5': 32, 's6': 48, 's7': 64},
           'touchTargetMin': 48, 'margin': {'compact': 16, 'medium': 24, 'expanded': 24},
           'gutter': {'compact': 8, 'medium': 16, 'expanded': 24}, 'columns': {'compact': 4, 'medium': 8, 'expanded': 12},
           'windowWidth': {'compact': '< 600', 'medium': '600 to 839', 'expanded': '>= 840'},
           'windowHeight': {'compact': '< 480', 'medium': '480 to 899', 'expanded': '>= 900'}}
SHAPE = {'unit': 'dp', 'none': 0, 'extraSmall': 4, 'small': 8, 'medium': 12, 'large': 16, 'extraLarge': 28, 'full': '50%'}
ELEV = {'unit': 'dp', 'level0': 0, 'level1': 1, 'level2': 3, 'level3': 6, 'level4': 8, 'level5': 12}
MOTION = {
  'duration': {'unit': 'ms', 'short1': 50, 'short2': 100, 'short3': 150, 'short4': 200, 'medium1': 250, 'medium2': 300,
               'medium3': 350, 'medium4': 400, 'long1': 450, 'long2': 500, 'long3': 550, 'long4': 600,
               'extraLong1': 700, 'extraLong2': 800, 'extraLong3': 900, 'extraLong4': 1000},
  'easing': {'emphasized': 'cubic-bezier(0.2, 0.0, 0.0, 1.0)', 'emphasizedDecelerate': 'cubic-bezier(0.05, 0.7, 0.1, 1.0)',
             'emphasizedAccelerate': 'cubic-bezier(0.3, 0.0, 0.8, 0.15)', 'standard': 'cubic-bezier(0.2, 0.0, 0.0, 1.0)',
             'standardDecelerate': 'cubic-bezier(0.0, 0.0, 0.0, 1.0)', 'standardAccelerate': 'cubic-bezier(0.3, 0.0, 1.0, 1.0)',
             'linear': 'cubic-bezier(0.0, 0.0, 1.0, 1.0)'},
  'reduceMotion': 'If Settings.Global.ANIMATOR_DURATION_SCALE is 0, all durations are 0 ms and decorative effects are off.',
}
full = {
  'meta': {'name': 'DropShot', 'version': '1.0.0', 'designSystem': 'Material Design 3',
           'toneModel': 'Tone = CIE L*. Hue and chroma in CIE LCh(ab), chroma reduced to fit sRGB.'},
  'color': {'palette': {k: {str(tn): v for tn, v in p.items()} for k, p in palettes.items()},
            'seed': {k: v[0] for k, v in PAL_DEF.items()},
            'scheme': schemes, 'semantic': sem, 'player': ply},
  'typography': TYPE, 'spacing': SPACING, 'shape': SHAPE, 'elevation': ELEV, 'motion': MOTION,
}
with open(os.path.join(OUT, 'design-tokens.json'), 'w') as fh: json.dump(full, fh, indent=2)

print('ALL PASS' if all_pass else 'SOME FAIL')
for name in schemes:
    for row in contrast_rows(name):
        if not row[-1]: print(name, row)
