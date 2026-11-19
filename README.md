# Frog Dissection — Interactive Undergraduate Zoology Laboratory

This repository contains a five-layer browser-based frog anatomy simulator with English/Tamil organ descriptions, guided dissection, interactive hotspots, specimen photographs, a spotter assessment and MCQs. Main entry point: **`index.html`**.

## Anatomical plates and views

| View | Source filename | Scientific hotspot review |
| --- | --- | --- |
| Layer 1 — external | `frog external.jpg` | Pending anatomical review |
| Layer 2 — musculature | `frog muscle.jpg` | Pending anatomical review |
| Layer 3 — viscera | `frog viscera.jpg` | Pending anatomical review |
| Layer 4 — heart/circulatory | `frog heart.jpg` | Pending anatomical review; heart photograph may not show every arterial/venous structure |
| Layer 5 — male urogenital | `Internal deep male.avif` | Pending anatomical review |
| Layer 5 — female urogenital | `Internal deep female.avif` | Pending anatomical review |

Additional assets: `Frog_Anatomy_Model_(50669580391).jpg` and `3840px-Frog_dissection,_5.jpg`. The last is the same binary photograph as `frog viscera.jpg`. These auxiliary files are not automatically assigned new interactive layers.

**Anatomical verification is independent of successful image rendering or browser CI.** Until a reviewer checks the visible organs, all default hotspot positions are provisional, including the male and female Layer 5 coordinates.

### Calibrating each plate

1. Open `index.html` in a browser served from the repository, so nearby relative image paths resolve. For example: `python -m http.server 8000`, then visit `http://localhost:8000`.
2. Select a layer. On Layer 5, use **Specimen: Male/Female** to select the distinct male and female photographs.
3. Choose **Calibrate Pins**. Select an organ and click its actual location in the photograph. **Do not place a marker on an organ that is not visible in that photograph.** The heart close-up and urogenital images may not display every organ in the shared organ database.
4. Repeat independently for every plate. **Export Pin Data (JSON)** to download `frog-calibration-review.json`; this includes separate `5_male` and `5_female` positions and notes local adjustments.
5. Submit the JSON and labelled specimen evidence for independent biological review **before integrating the coordinates into repository defaults**. Local browser calibration does not automatically update GitHub, and an adjusted pin does not imply biological validation.
6. If a specimen photo is replaced through **Upload Plate Photo**, its saved pin alignment is reset. Other plates are unaffected.

The `Export Single-File HTML` button does **not** embed repository photograph files automatically. Keep image files alongside the exported HTML unless every needed image was embedded through user uploads; even then, verify all views.

### Scientific review criteria

- Confirm specimen orientation and whether a structure is actually exposed.
- **Male:** testes, mesonephric kidneys, fat bodies, urinogenital/Wolffian ducts and urinary bladder. Do not depict female oviducts in male specimens.
- **Female:** ovaries, **separate coiled oviducts**, kidneys/ureters, fat bodies and urinary bladder. Distinguish coiled oviducts from slender ureters.
- Avoid a brain hotspot in an exposed ventral urogenital specimen.
- For heart-only photographs, show labels only for structures visible in the actual plate; do not imply that general systemic/venous anatomy is demonstrated by a cropped heart.
- Frogs have lung **faveoli**, not mammalian-style terminal alveoli.

Reference resources for reviewer orientation:
- [NCERT/Tamil Nadu Class XI Zoology: structural organisation of the frog](https://obj.diksha.gov.in/ntp-content-production/content/assets/do_312915913512009728173/b167_std_11_bio_zoology_em.pdf)
- [Biology LibreTexts — Amphibian (Frog) Dissection](https://bio.libretexts.org/Courses/Northeast_Wisconsin_Technical_College/General_Biology_Laboratory_Manual/Laboratory_14%3A_Vertebrates/14.02%3A_Amphibian_%28Frog%29_Dissection)
- [Biology LibreTexts — Frog Dissection: External and Internal](https://bio.libretexts.org/Learning_Objects/Worksheets/Book%3A_The_Biology_Corner_%28Worksheets%29/Anatomy_Worksheets/Frog_Dissection%3A_External_and_Internal)

## Tests

The GitHub Actions workflow runs:
- `node --test tests/frog-smoke.test.cjs` — JavaScript syntax, image presence and sex-specific code checks.
- `node tests/browser-smoke.cjs` — Chromium decoding, male/female photo switching, pin independence, calibration export, quiz and guided navigation.
- Existing Jekyll build.

Browser and source checks **do not certify** specimen identification, image permissions, publication rights or every tool behaviour. A plate-by-plate anatomical and image-rights audit remains required for classroom release.

## Image rights

Files are committed, but a repository upload does **not** establish reuse permission. Before redistribution or publication, record original creator/source, licence, required attribution and whether the image was modified for each plate.
