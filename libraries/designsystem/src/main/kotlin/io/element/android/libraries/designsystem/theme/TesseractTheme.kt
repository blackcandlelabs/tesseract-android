/*
 * Tesseract fork: Black Candle theme overrides.
 *
 * Ground-up redesign foundation (2026-10-15): the app's semantic colours are
 * overridden at the single theme composition point (ElementThemeApp) with the
 * Black Candle ID v2 token set — near-black warm surfaces, candle-amber
 * accent, the ink ramp — matching the Figma file "Tesseract - Ground-Up
 * Redesign" and the web theme in tesseract-web. Upstream generated token
 * files are untouched; everything lives in these copy() overrides.
 */
package io.element.android.libraries.designsystem.theme

import androidx.compose.ui.graphics.Color
import io.element.android.compound.tokens.generated.SemanticColors

private val CandleAmber = Color(0xFFE9A33D)
private val CandleAmberBright = Color(0xFFF0B35C)
private val CandleAmberPressed = Color(0xFFC9852A)
private val CandleAmberDeep = Color(0xFFB87A1E)
private val CandleAmberDeepHovered = Color(0xFFA66C1A)
private val CandleAmberDeepPressed = Color(0xFF8F5E15)
private val InkOnAmber = Color(0xFF171209)

fun SemanticColors.tesseractDark(): SemanticColors = copy(
    bgCanvasDefault = Color(0xFF0A0A0D),
    bgCanvasDefaultLevel1 = Color(0xFF141419),
    bgCanvasDisabled = Color(0xFF141419),
    bgSubtlePrimary = Color(0xFF141419),
    bgSubtleSecondary = Color(0xFF1C1C22),
    bgSubtleTertiary = Color(0xFF26262E),
    bgActionPrimaryRest = CandleAmber,
    bgActionPrimaryHovered = CandleAmberBright,
    bgActionPrimaryPressed = CandleAmberPressed,
    bgActionPrimaryDisabled = Color(0xFF3A3A42),
    bgActionSecondaryRest = Color(0xFF1C1C22),
    bgActionSecondaryHovered = Color(0xFF26262E),
    bgActionSecondaryPressed = Color(0xFF2E2E36),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFFA1A1A6),
    textDisabled = Color(0xFF6E6E73),
    textOnSolidPrimary = InkOnAmber,
    textActionAccent = CandleAmberBright,
    textActionPrimary = CandleAmberBright,
    textLinkExternal = CandleAmberBright,
    textCriticalPrimary = Color(0xFFE5534B),
    textSuccessPrimary = Color(0xFF57AB5A),
    iconPrimary = Color(0xFFF5F5F7),
    iconSecondary = Color(0xFFA1A1A6),
    iconQuaternary = Color(0xFF6E6E73),
    iconAccentPrimary = CandleAmber,
    iconAccentTertiary = CandleAmber,
    iconOnSolidPrimary = InkOnAmber,
    borderInteractivePrimary = Color(0xFF2E2E36),
    borderInteractiveHovered = Color(0xFF3D3D47),
    borderInteractiveSecondary = Color(0xFF26262E),
    borderAccentPrimary = CandleAmber,
    borderAccentSubtle = CandleAmberPressed,
    borderFocused = CandleAmberBright,
)

fun SemanticColors.tesseractLight(): SemanticColors = copy(
    bgCanvasDefault = Color(0xFFFBFBFC),
    bgCanvasDefaultLevel1 = Color(0xFFFFFFFF),
    bgSubtlePrimary = Color(0xFFFFFFFF),
    bgSubtleSecondary = Color(0xFFF2F2F5),
    bgActionPrimaryRest = CandleAmberDeep,
    bgActionPrimaryHovered = CandleAmberDeepHovered,
    bgActionPrimaryPressed = CandleAmberDeepPressed,
    bgActionPrimaryDisabled = Color(0xFFD8D8DE),
    textPrimary = Color(0xFF141419),
    textSecondary = Color(0xFF515154),
    textDisabled = Color(0xFF86868B),
    textOnSolidPrimary = Color(0xFFFFFFFF),
    textActionAccent = CandleAmberDeep,
    textActionPrimary = CandleAmberDeep,
    textLinkExternal = CandleAmberDeep,
    iconPrimary = Color(0xFF141419),
    iconSecondary = Color(0xFF515154),
    iconQuaternary = Color(0xFF86868B),
    iconAccentPrimary = CandleAmberDeep,
    iconAccentTertiary = CandleAmberDeep,
    iconOnSolidPrimary = Color(0xFFFFFFFF),
    borderInteractivePrimary = Color(0xFFD9D9DE),
    borderInteractiveHovered = Color(0xFFC4C4CC),
    borderInteractiveSecondary = Color(0xFFE4E4E9),
    borderAccentPrimary = CandleAmberDeep,
    borderFocused = CandleAmberDeep,
)
