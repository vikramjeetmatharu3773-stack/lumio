package app.lumio.player.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/** Central motion tokens. Spec: docs/05_ANIMATIONS_AND_MOTION.md */
@Immutable
class MotionSpec(val enabled: Boolean = true, val speed: Float = 1f, val haptics: Boolean = true) {
    private val k get() = speed.coerceIn(0.5f, 2f)

    fun ms(base: Int): Int = if (!enabled) 0 else (base / k).toInt()

    fun <T> tween(base: Int, easing: Easing = FastOutSlowInEasing): FiniteAnimationSpec<T> =
        if (!enabled) snap() else tween((base / k).toInt(), easing = easing)

    fun <T> bounce(): FiniteAnimationSpec<T> = if (!enabled) snap() else spring(dampingRatio = 0.55f, stiffness = 500f)
    fun <T> smooth(): FiniteAnimationSpec<T> = if (!enabled) snap() else spring(dampingRatio = 0.82f, stiffness = 380f)
    fun <T> gentle(): FiniteAnimationSpec<T> = if (!enabled) snap() else spring(dampingRatio = 0.9f, stiffness = 200f)
    fun <T> liquid(): FiniteAnimationSpec<T> = if (!enabled) snap() else spring(dampingRatio = 0.5f, stiffness = 350f)
}

val LocalMotion = compositionLocalOf { MotionSpec() }
