# Frog Dissection Native — Final Engineering and Zoology Audit
**Date:** 19 November 2026  
**Scope:** `native/compose-frog-dissection-20261119`, source of the pure Kotlin/Jetpack Compose module `native-android/`.  
**Decision:** **QA ONLY — not approved for unqualified classroom release or Play Store submission.**

## Scope and evidence
- Source inspected: Kotlin UI, corpus parser, JSON asset data, marker geometry, Android manifest and Gradle configurations.
- Content inventory at review: 5 anatomical layers, **6** photographs (sex-specific Layer 5), **31** organ records, **26** MCQs and **6** guided steps.
- The previous HTML/WebView implementation is stored separately in `android-app/`. The **`native-android/`** module contains no WebView code or HTML runtime.
- Earlier device photographs demonstrate that the initial Compose build rendered the anatomy images but had an excessively tall toolbar, clipped layer controls and oversized/clustered markers. The corresponding corrections are in the native branch, but a follow-up screenshot comparison on the actual phone has not been accepted.
- Baseline exact-commit GitHub Actions run [44480399161](https://github.com/rameshgascngl-create/Frog-dissection-/actions/runs/44480399161): success. It predates the additional final-audit changes.

## Engineering findings

| Area | Audit finding / action | Status |
|---|---|---|
| Native architecture | Kotlin, Compose, Material 3, no WebView; legacy hybrid module retained | PASS (source) |
| Offline assets | Five anatomical layers, male and female AVIF plates, content JSON packaged by CI | PASS (build-level) |
| Repository organisation | Native project isolated; verified image count, corpus and Gradle build | PASS |
| Layer navigation | Replaced clipped layer chips with five fixed-width selection controls | SOURCE CORRECTED; device recheck |
| Header / mobile typography | Compact native header, fewer wrapped labels | SOURCE CORRECTED; 200% font test pending |
| Photo overlays | Pixel-bounded markers now avoid blocking the original printed specimen labels | SOURCE CORRECTED; anatomical alignment pending |
| Photo gestures | Native pinch zoom and pan implemented | PRESENT; device interaction not accepted |
| Male/female specificity | Male and female photographs remain separate; independent calibration and reproductive descriptions implemented | SOURCE CORRECTED; scientific review pending |
| Anatomy glossary | Thirty-one organ records searchable; selecting oviduct opens female Layer 5 | SOURCE CORRECTED |
| MCQ grading | Removed indefinite wraparound after question 26; completion and reset provided; state is rotation-saveable | SOURCE CORRECTED; emulator verification pending |
| Spotter scoring | Practice now terminates, resets and explicitly disclaims unverified photographic positions | SOURCE CORRECTED; not a graded exam |
| Guided steps | Six steps retained; available tool actions record interaction state | PARTIAL SIMULATOR |
| Tissue removal | Forceps merely changes marker/retraction state; underlying photography is not modified | **RELEASE BLOCKER** for claims of complete dissection simulation |
| Rotation and process death | Primitive UI and quiz state uses rememberSaveable; calibration uses local preferences | PARTIAL; exercise list and image gesture state still need recovery tests |
| Accessibility | Material controls have native semantics; individual Canvas pins are not independently TalkBack-addressable | **BLOCKER** pending accessibility improvements |
| Android data protection | No Internet permission; backup disabled; no analytics SDK; in-APK privacy statement included | SOURCE REVIEW PASS; store review still required |
| Google Play API policy | Upgraded native module to compile/target SDK 36 with Android Gradle Plugin 8.11.1 and Gradle 8.13; new API 34/API 36 emulator matrix added | SOURCE CORRECTED; exact build validation pending |
| Signing and store | CI generates a debug APK, not a signed production AAB | NOT RELEASE READY |
| Photos and attribution | Image rights evidence incomplete; muscular composite includes a Jenna Hellack Fall 2000 attribution lead, not a licence | **RELEASE BLOCKER** |

## Scientific and Tamil editorial checks

- **Corrected:** female ovarian inspector text is now separate from male testicular descriptions; separate female oviduct pin starts deliberately unplaced.
- **Corrected:** MCQ 25 now tests the upper-jaw and vomerine tooth-bearing regions instead of treating `acrodont` as the single diagnostic property of anuran dentition; the explanation acknowledges the typically pedicellate, replaceable dentition.
- **Preserved:** all 26 questions and 31 organ records, with no summary replacement.
- **Not yet accepted:** photo-by-photo marker positions, all six image reuse rights, full corpus peer review and a formal Tamil Nadu Zoology editorial glossary check.
- **Source audit updates:** male/female reproductive inspector content is now sex-specific; MCQ and photo spotter terminate after one round and present practice/score summaries; in-app privacy disclosure is scrollable; glossary oviduct selection opens the female plate.
- **Tamil coverage limitation:** Tamil organ labels are present, but much of the explanatory morphology, function, viva and MCQ material remains English. This is **not** a complete bilingual textbook.
- **Nervous system limitation:** a dorsal brain record is supplementary text, not visible in the Layer 5 ventral urogenital specimen.

## Automated validation

- Build workflow: [Pure Native Frog Dissection Android QA](https://github.com/rameshgascngl-create/Frog-dissection-/actions/workflows/native-compose-apk.yml), now requires JVM corpus/marker/assessment tests, native APK compilation, and Android UI-test APK compilation.
- New separate workflow: [Frog Native Android Emulator Acceptance](https://github.com/rameshgascngl-create/Frog-dissection-/actions/workflows/native-device-acceptance.yml), intended to execute Compose UI tests on API 34 **and API 36** at 100% and 200% font scaling. Its test invocation working-directory issue was corrected; results must be confirmed for the final commit.
- **An emulator check is not a pass until the exact job finishes SUCCESS.** A debug APK passing compilation does not establish runtime/performance/physical-device acceptance.

## Latest audited QA build

- **Result:** SUCCESS for native JVM unit tests, Compose instrumentation-test APK compilation, offline asset packaging, APK compilation and artifact publishing.
- **Exact application-code commit:** `620bd01314e664516a8e1afdf57e721246eb4ff6`
- **Workflow:** [#44483812063](https://github.com/rameshgascngl-create/Frog-dissection-/actions/runs/44483812063)
- **APK artifact:** [FrogDissection-Pure-Native-Compose-QA-APK #14509806320](https://github.com/rameshgascngl-create/Frog-dissection-/actions/runs/44483812063/artifacts/14509806320)
- **Version:** `2.0.3-native-audit-qa`, native package `org.gasczoology.frogdissection.nativeapp`; `compileSdk=36`, `targetSdk=36`, `minSdk=31`.
- **SHA-256:** `c918ed0339dde08e161af336102c25aa22f4e0afdccc3ebe3a97050ea705dab4`
- **Emulator acceptance now verified:** [workflow #44483812089](https://github.com/rameshgascngl-create/Frog-dissection-/actions/runs/44483812089) returned SUCCESS for API 34 and API 36, both 100% and 200% font settings. Evidence artifacts: API 34 #14510545426; API 36 #14510610086.
- **Next native accessibility QA candidate:** v2.0.4 code adds screen-reader actions for every visible organ pin, four directional pin-adjustment controls, and rotation-safe forceps state. Build and emulator reacceptance require an exact-commit PASS before distributing as an updated QA APK.

## Accessibility follow-up

Canvas-drawn pins did not have individual TalkBack actions in v2.0.3; the native photo now exports labeled `CustomAccessibilityAction` handlers. A screen-reader user can inspect an individual organ through the photograph's accessibility actions or through the organ-list chips. During calibration, an additional four-direction control permits measured adjustments without pixel-perfect tapping. Tests exercise the named actions and directional controls; final TalkBack usability must still be checked on a device.

The new QA candidate retains original unapproved source coordinates and six educational photographs. **No scientific or copyright sign-off is implied** by accessibility test success.

## Release gates remaining

1. Achieve SUCCESS on exact-commit JVM, APK and instrumentation compilation and **API 34 plus API 36** emulators at both font scales, then verify on the user's actual Android device.
2. Confirm all six specimen photos and calibrate every visible organ hotspot against the actual pixel coordinates, with review evidence and reviewer sign-off.
3. Resolve reuse licences/creator attribution for all bundled photographs, or replace with appropriately licensed originals.
4. Make canvas hotspots individually discoverable and operable with TalkBack/Switch Access; test at larger fonts and in landscape.
5. Implement genuinely interactive tissue-visibility progression before describing the exercise as complete dissection, or explicitly market it as an anatomical plate explorer with virtual tools.
6. Complete Tamil explanatory-language review and scientific MCQ editorial acceptance.
7. Provide production signing, AAB, screenshots, app listing, public privacy-policy location and store compliance checks.

**Release decision:** Use only the verified debug QA APK for testing. Do not label the current app an approved, fully simulated, published or scientifically signed-off practical manual.
