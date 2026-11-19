package org.gasczoology.frogdissection.nativeapp

/**
 * Pixel-space marker sizes for photographic anatomy.
 * The images contain dense structures and many already have printed labels.
 * Markers indicate a selectable location; they must not obscure the tissue.
 */
internal fun markerRadiusPx(shortSidePixels: Float, selected: Boolean): Float {
    if (shortSidePixels <= 0f) return 0f
    return if (selected) (shortSidePixels * .026f).coerceIn(5f, 10f)
    else (shortSidePixels * .011f).coerceIn(2.5f, 5f)
}
