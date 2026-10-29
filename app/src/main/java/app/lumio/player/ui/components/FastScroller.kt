package app.lumio.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val LETTERS = ('A'..'Z').toList() + '#'

/**
 * Alphabet fast scroller. [firstIndexByLetter] maps a letter to the first list index (before [indexOffset]) that starts with it.
 * Letters without songs jump to the next available letter.
 */
@Composable
fun FastScroller(
    firstIndexByLetter: Map<Char, Int>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    indexOffset: Int = 0,
) {
    val on = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf<Char?>(null) }
    // for each letter: index of first letter >= it that exists
    val jump = remember(firstIndexByLetter) {
        LETTERS.mapIndexed { i, _ ->
            (i until LETTERS.size).firstNotNullOfOrNull { j -> firstIndexByLetter[LETTERS[j]] } ?: firstIndexByLetter.values.maxOrNull() ?: 0
        }
    }
    Box(modifier.width(28.dp).fillMaxHeight(0.78f), contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(24.dp)
                .pointerInput(jump) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun handle(y: Float) {
                            val i = (y / size.height * LETTERS.size).toInt().coerceIn(0, LETTERS.lastIndex)
                            active = LETTERS[i]
                            scope.launch { listState.scrollToItem(jump[i] + indexOffset) }
                        }
                        handle(down.position.y)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { if (it.pressed) { handle(it.position.y); it.consume() } }
                        } while (event.changes.any { it.pressed })
                        active = null
                    }
                },
        ) {
            LETTERS.forEach { ch ->
                Text(
                    ch.toString(), fontSize = 10.sp, textAlign = TextAlign.Center,
                    color = if (ch == active) accent else on.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f).width(24.dp),
                )
            }
        }
        active?.let {
            Box(
                Modifier.align(Alignment.CenterStart).offset(x = (-64).dp).size(56.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center,
            ) { Text(it.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall) }
        }
    }
}
