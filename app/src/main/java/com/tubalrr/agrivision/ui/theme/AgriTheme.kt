package com.tubalrr.agrivision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class AgriPalette(
    val cream: Color,
    val card: Color,
    val green: Color,
    val greenDeep: Color,
    val greenSoft: Color,
    val sage: Color,
    val gold: Color,
    val goldSoft: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val warning: Color,
    val danger: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val surfaceVariant: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val isDark: Boolean
)

internal val AgriFieldPalette = AgriPalette(
    cream = Color(0xFFF3F6F0),
    card = Color(0xFFFFFFFF),
    green = Color(0xFF267247),
    greenDeep = Color(0xFF123D2A),
    greenSoft = Color(0xFFDFF1E4),
    sage = Color(0xFFA8C8B0),
    gold = Color(0xFFE2B34B),
    goldSoft = Color(0xFFFFF2D2),
    text = Color(0xFF192B20),
    muted = Color(0xFF637267),
    line = Color(0xFFDEE6DC),
    warning = Color(0xFF96610F),
    danger = Color(0xFFB53D37),
    onPrimary = Color.White,
    onSecondary = Color(0xFF192B20),
    surfaceVariant = Color(0xFFEDF2EB),
    errorContainer = Color(0xFFFCE8E5),
    onErrorContainer = Color(0xFF761F1A),
    isDark = false
)

// AgriVision Pro: deep charcoal-green surfaces, luminous crop green and restrained brass.
internal val AgriProPalette = AgriPalette(
    cream = Color(0xFF0B1410),
    card = Color(0xFF14221A),
    green = Color(0xFF8BD39B),
    greenDeep = Color(0xFF07110B),
    greenSoft = Color(0xFF24412D),
    sage = Color(0xFF9DBCA4),
    gold = Color(0xFFE7B950),
    goldSoft = Color(0xFF3D321B),
    text = Color(0xFFEAF2EB),
    muted = Color(0xFFA2B1A5),
    line = Color(0xFF2B3B30),
    warning = Color(0xFFF0C36A),
    danger = Color(0xFFFF9B91),
    onPrimary = Color(0xFF082013),
    onSecondary = Color(0xFF2C2108),
    surfaceVariant = Color(0xFF1C2C22),
    errorContainer = Color(0xFF512522),
    onErrorContainer = Color(0xFFFFDAD5),
    isDark = true
)

internal val LocalAgriPalette = staticCompositionLocalOf { AgriFieldPalette }

internal val AgriCream: Color
    @Composable get() = LocalAgriPalette.current.cream
internal val AgriCard: Color
    @Composable get() = LocalAgriPalette.current.card
internal val AgriGreen: Color
    @Composable get() = LocalAgriPalette.current.green
internal val AgriGreenDeep: Color
    @Composable get() = LocalAgriPalette.current.greenDeep
internal val AgriGreenSoft: Color
    @Composable get() = LocalAgriPalette.current.greenSoft
internal val AgriSage: Color
    @Composable get() = LocalAgriPalette.current.sage
internal val AgriGold: Color
    @Composable get() = LocalAgriPalette.current.gold
internal val AgriGoldSoft: Color
    @Composable get() = LocalAgriPalette.current.goldSoft
internal val AgriText: Color
    @Composable get() = LocalAgriPalette.current.text
internal val AgriMuted: Color
    @Composable get() = LocalAgriPalette.current.muted
internal val AgriLine: Color
    @Composable get() = LocalAgriPalette.current.line
internal val AgriWarning: Color
    @Composable get() = LocalAgriPalette.current.warning
internal val AgriDanger: Color
    @Composable get() = LocalAgriPalette.current.danger
