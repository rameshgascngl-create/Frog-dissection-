# Frog anatomical photograph acceptance register

Updated: 19 November 2026

**Purpose:** Plate-by-plate evidence required before treating the simulator’s provisional hotspots as anatomically validated. Successful Chromium decoding and CI screenshots are engineering evidence only.

| Review ID | Layer/view | Repository asset | Anatomical points to verify | Image creator/source | Licence/permission | Hotspot positions | Status |
|---|---|---|---|---|---|---|---|
| FROG-P01 | External | `frog external.jpg` | Nares, eyes, tympana, fore-/hindlimbs, cloaca; vocal sac visibility and sex | Not recorded | Not verified | Provisional | HOLD |
| FROG-P02 | Muscular | `frog muscle.jpg` | Pectoral region, rectus abdominis, linea alba, exposed limb muscles | Not recorded | Not verified | Provisional | HOLD |
| FROG-P03 | Visceral | `frog viscera.jpg` | Liver, gall bladder, stomach, intestine, lungs, heart, fat bodies; kidneys only if visible | Not recorded | Not verified | Provisional | HOLD |
| FROG-P04 | Circulatory | `frog heart.jpg` | Both atria, single ventricle, conus/arterial outlet; only label systemic arches or portal vessels if actually visible | Not recorded | Not verified | Provisional | HOLD |
| FROG-P05 | Male urogenital | `Internal deep male.avif` | Testes, kidneys, fat bodies, male urinogenital duct, urinary bladder; do not label an unseen brain | Not recorded | Not verified | Provisional | HOLD |
| FROG-P06 | Female urogenital | `Internal deep female.avif` | Distinguish ovaries from coiled oviducts; ureters, kidneys, urinary bladder, fat bodies | Not recorded | Not verified | Provisional; oviduct deliberately unplaced | HOLD |

## Required reviewer evidence

1. Open the latest [GitHub Actions workflow](https://github.com/rameshgascngl-create/Frog-dissection-/actions) and download the **frog-anatomical-plate-review** artifact. It contains six full-layer PNG screenshots and `plate-index.json` with source dimensions, rendered overlay counts and view names.
2. Inspect the screenshot and original asset together. Decide independently whether each listed organ is visible and the marker is accurate. A text label alone cannot establish that the organ exists in the frame.
3. Enter reviewed coordinates using the **Calibrate Pins** control in the simulator; export `frog-calibration-review.json`. Distinct `5_male` and `5_female` coordinates must be retained.
4. Record for each approved plate: original creator, stable source URL, usage licence or written permission, required attribution, original image dimensions, any edits/crops, zoological reviewer, review date, and the exact SHA of the source image being approved.
5. Do not promote positions from **provisional** to **approved** until a zoology reviewer has matched the labels and exported coordinates against the actual pixels. Do not infer missing coordinates.

## Specific release risks

- The general Layer 4 anatomy database contains structures that may not appear in a heart close-up. Disable or mark such hotspots unplaced where the structure is absent.
- Default hotspot coordinates across all layers originated before the current uploaded photographs were individually calibrated.
- Repository image names and existing uploads alone do not establish copyright licence or educational redistribution rights.
- A screenshot artifact is visual evidence for review, **not** a scientific acceptance certificate.
