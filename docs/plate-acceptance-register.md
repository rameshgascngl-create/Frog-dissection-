# Frog anatomical photograph acceptance register

Updated: 19 November 2026

**Purpose:** Plate-by-plate evidence required before treating the simulator’s provisional hotspots as anatomically validated. Successful Chromium decoding and CI screenshots are engineering evidence only.

| Review ID | Layer/view | Repository asset | Anatomical points to verify | Image creator/source | Licence/permission | Hotspot positions | Status |
|---|---|---|---|---|---|---|---|
| FROG-P01 | External | `frog external.jpg` | Nares, eyes, tympana, fore-/hindlimbs, cloaca; vocal sac visibility and sex | Not recorded | Not verified | Provisional | HOLD |
| FROG-P02 | Muscular | `frog muscle.jpg` | Pectoral region, rectus abdominis, linea alba, exposed limb muscles | Jenna Hellack, Fall 2000 (matching slide transcription; see below) | Not verified | Provisional | HOLD |
| FROG-P03 | Visceral | `frog viscera.jpg` | Liver, gall bladder, stomach, intestine, lungs, heart, fat bodies; kidneys only if visible | Not recorded | Not verified | Provisional | HOLD |
| FROG-P04 | Circulatory | `frog heart.jpg` | Both atria, single ventricle, conus/arterial outlet; only label systemic arches or portal vessels if actually visible | Not recorded | Not verified | Provisional | HOLD |
| FROG-P05 | Male urogenital | `Internal deep male.avif` | Testes, kidneys, fat bodies, male urinogenital duct, urinary bladder; do not label an unseen brain | Not recorded | Not verified | Provisional | HOLD |
| FROG-P06 | Female urogenital | `Internal deep female.avif` | Distinguish ovaries from coiled oviducts; ureters, kidneys, urinary bladder, fat bodies | Not recorded | Not verified | Provisional; oviduct deliberately unplaced | HOLD |

## Visual inspection findings (19 November 2026)

The following observations are grounded in inspection of the repository JPEG plate pixels. They are **not a full acceptance of the anatomical hotspot positions**.

| Plate | Verified visual issue | Corrective action implemented | Outstanding check |
|---|---|---|---|
| FROG-P01 | A lateral external photograph rather than the assumed ventral specimen | Repositioned visible nares, eye, tympanum and limbs; withheld the cloaca and vocal sac hotspots because they are not visibly exposed | Independently review each position on the source pixels |
| FROG-P02 | A composite two-view "Frog ventral anterior muscles" teaching page with printed figure keys and visible "Jenna Hellack Fall 2000" credit | Moved pectoral, rectus and linea-alba provisional targets towards the primary view; withheld deep hindlimb target | Confirm photo permission and improve crop/plate selection |
| FROG-P04 | A close-up image already printed with aortic arches, right/left atrium, truncus arteriosus, ventricle and lung labels | Improved provisional overlay positions using visual landmarks; withheld unsupported portal-venous hotspot | Verify organ targets against original pixels and avoid overlapping annotation arrows |
| FROG-P03, P05, P06 | Full visual sign-off not completed in this checkpoint | Preserve provisional status and separate calibration/exports | Review source photograph pixels and confirm sex-specific visible structures |

**Attribution evidence:** The muscular image contains the text "Jenna Hellack Fall 2000". I also located a matching slide transcription, [Frog Dissection — Jenna Hellack, Fall 2000](https://studyres.com/doc/8495744/frog-dissection---powerpoint-presentation), which explicitly includes "Frog ventral anterior muscles" and identifies pectoralis, rectus abdominis, linea alba and external oblique. This strengthens the creator/source attribution but **does not establish a reuse licence**. Obtain permission or replace with a licensed/original plate before public redistribution.

**Image identity:** The Git blob SHA for `frog viscera.jpg` and `3840px-Frog_dissection,_5.jpg` is identical: `03e1bcc0172992cbc7df8e776cc9027185d0ddf4`. Do not count them as separate anatomical views.

**Maintenance note:** Old browser-local calibrations may still override newer defaults. For teaching, clear/verify local calibration settings or use a fresh browser profile before checking corrected provisional positions. Coordinates are not authoritatively signed off.

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
