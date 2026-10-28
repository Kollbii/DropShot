# DropShot player frame concepts

Open [index.html](index.html) to compare 15 frame concepts on 4 player colors. The gallery shows each frame on a wide and a compact score panel. [gallery.png](gallery.png) is a static overview.

The SVG files are the source concepts. The app renders equivalent scalable frames with Android Canvas in `Android/lootbox/src/main/java/pl/maciek/squashscoreboard/lootbox/RewardFrames.java`. All 15 frames are in the shared lootbox pool and can be equipped per player.

Frames 01, 02, 05, 06, and 10 are Purple tier. The other 10 are Green tier.

## Suggested shortlist

| Number | Frame | Why it stays legible at small sizes |
|---|---|---|
| 01 | Photon Blade | Continuous bright line and distinct corner handles. |
| 02 | Tribal Fang | Large, symmetric shapes at the corners. |
| 03 | Barbed Wire | Bold cables and spaced barbs. |
| 04 | Forged Steel | Strong bevel and large rivets. |
| 05 | Neon Circuit | Few traces and clear nodes. |
| 06 | Arcane Runes | Simple symbols and a stable double outline. |
| 07 | Thorn Vine | Large leaves and a continuous vine. |
| 08 | Pixel Glitch | Square steps survive pixel reduction. |
| 09 | Racing Stripes | Diagonal bands have high contrast. |
| 10 | Royal Filigree | Main gold outline stays visible if flourishes fade. |

11 through 15 are alternatives. Chrome, fire, crystal, hex texture, and glass shards have more small details.

## Scaling plan

- Each source is a transparent SVG with a `640 × 360` view box. The center contains no artwork.
- The gallery uses a 48-unit 9-slice and a 26-pixel rendered edge. This is an approximate browser preview.
- The app preserves the full corner ornaments as fixed-size blocks. It extends the straight edge between corners and repeats barbs, runes, and hexes with fixed spacing.
- Keep the visible stroke at least 2 physical pixels at the smallest supported panel size. Hide secondary marks when they become thinner than 2 pixels.
- Keep names and scores inside the safe content area, at least 26 display pixels from the panel edge in this gallery.
- Android draws corners and edges independently at the target density. The app does not stretch one bitmap over the whole panel.

Run `node generate.js` after edits to the source definitions. It writes the 15 SVG files and `index.html`.
