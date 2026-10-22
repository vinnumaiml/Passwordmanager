# ELVARIX Brand Identity & Design System Specification

## 1. Executive Vision

**ELVARIX** represents the bleeding edge of sovereign digital security. It is engineered for discerning individuals and security architects who demand uncompromising zero-knowledge protection, algorithmic certainty, and cinematic, high-craft aesthetics.

The visual identity is anchored on **The Elvarix Core** — an original geometric mark constructed from interlocking orbital protective ribbons that form a futuristic letter **E**, framing an intentional negative-space cryptographic keyway aperture.

---

## 2. The Elvarix Core — Geometric Breakdown

```
        376,116 ─────── 180,116
           /               \
       400,152              124,172
          │                    │
          │     164,232 ── 324,232
          │        │       [CORE] \
          │     164,280 ── 324,280/ 356,256
          │                    │
       400,360              124,340
           \               /
        376,396 ─────── 180,396
```

### Visual Metaphors
- **Protection & Perimeter:** The outer orbital spine establishes an impenetrable boundary around sensitive credentials.
- **Precision & Engineering:** 45-degree chamfers and aerospace-grade facets communicate rigorous mathematics and bug-free execution.
- **Negative-Space Keyway:** At the tip of the central core tongue, a micro-aperture forms a cryptographic keyhole slot without relying on generic consumer padlock cliches.
- **Interlocking Orbital Flow:** An electric cyan secondary ribbon wraps the right flank, symbolizing dynamic cryptographic handshakes and live synchronization.

---

## 3. Official Color Matrix

| Role | Color Name | Hex | RGB | Usage |
| :--- | :--- | :--- | :--- | :--- |
| **Primary Background** | Midnight Obsidian | `#080A14` | `rgb(8, 10, 20)` | Core app background, canvas, splash |
| **Secondary Dark** | Deep Slate | `#10162A` | `rgb(16, 22, 42)` | Elevated surfaces, cards, modals |
| **Elevated Surface** | Quantum Indigo | `#161E35` | `rgb(22, 30, 53)` | Input fields, active toggles |
| **Primary Brand Accent** | Luminous Violet | `#8B5CF6` | `rgb(139, 92, 246)` | Primary buttons, active glow, primary logo crest |
| **Electric Accent** | Electric Cyan | `#43E7D0` | `rgb(67, 231, 208)` | Keyway highlights, security badges, tags, success states |
| **Base Typography** | Starlight White | `#F4F6FF` | `rgb(244, 246, 255)` | Primary headlines, wordmark, high-contrast labels |
| **Muted Technical** | Technical Silver | `#A5B0CC` | `rgb(165, 176, 204)` | Secondary copy, metadata, borders |

### Core Gradient
- **Angle / Vector:** `(120, 110)` to `(420, 400)` (Linear diagonal top-left to bottom-right)
- **Stops:**
  - `0%` — `#8B5CF6` (Violet)
  - `55%` — `#6366F1` (Cybernetic Indigo)
  - `100%` — `#43E7D0` (Electric Cyan)

---

## 4. Typography & Wordmark Structure

### Wordmark
- **Text:** `ELVARIX`
- **Case:** Strict uppercase (`ELVARIX`, never `Elvarix` in logo marks)
- **Weight:** Black / Heavy (800)
- **Tracking / Letter Spacing:** `+9px` at 64px scale (approx `+140` tracking)
- **Typeface Families:** System Sans-Serif, SF Pro Display, Inter, Roboto

### Sub-Brand Descriptor
- **Text:** `ZERO-KNOWLEDGE CIPHER VAULT`
- **Tracking:** `+5.5px` at 13px scale
- **Color:** `#43E7D0` (Electric Cyan)

---

## 5. Composition Configurations

1. **Standalone Symbol (`elvarix-symbol.svg`):**
   - Minimum digital size: `24px x 24px`
   - Optimal display size: `48px` to `512px`
2. **Horizontal Wordmark Lockup (`elvarix-logo.svg`):**
   - Proportions: 1:3.8 ratio (`760px x 200px`)
   - Symbol aligned vertically with the uppercase cap height of `ELVARIX`.
3. **Stacked Vertical Lockup (`elvarix-logo-stacked.svg`):**
   - Centered alignment.
   - Recommended for splash screens, packaging, and hero displays.
4. **Monochrome Symbol (`elvarix-symbol-mono.svg`):**
   - For laser engraving, physical stencils, and monochrome system status bars.

---

## 6. Android Adaptive Icon Guidelines

Per Android Adaptive Icon specifications:
- **Canvas Size:** `108dp x 108dp`
- **Safe Zone:** Center `72dp x 72dp` (inner circle / squircle)
- **Background (`ic_launcher_background.xml`):** Deep Obsidian gradient `#080A14` to `#10162A`.
- **Foreground (`ic_launcher_foreground.xml`):** Elvarix Core centered strictly within the 72dp diameter safe radius.
- **Monochrome (`ic_launcher_monochrome.xml`):** Supports Material You Dynamic Theming on Android 13+.

---

## 7. Brand Restrictions & Anti-Patterns

- **NEVER** substitute The Elvarix Core with generic locks, padlocks, or consumer shields.
- **NEVER** enclose the logo in a basic circular badge.
- **NEVER** warp, stretch, or alter the 45-degree chamfer ratios.
- **NEVER** render the wordmark in low-contrast gray or pastel shades.
- **NEVER** add excessive neon bloom or drop shadows that compromise silhouette clarity.
