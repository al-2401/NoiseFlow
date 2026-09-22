package app.noiseflow.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.noiseflow.designsystem.theme.ControlStyle
import app.noiseflow.designsystem.theme.LocalSkin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Tone control: one continuous spectral slope, from deep rumble to bright
 * hiss. The whole product hangs off this widget, so it is in the design system
 * rather than in a feature module, and every skin renders the same value.
 */
@Composable
fun ToneControl(
    slope: Float,
    onSlopeChange: (Float) -> Unit,
    label: String,
    valueDescription: String,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = DEFAULT_RANGE,
) {
    when (LocalSkin.current.control) {
        ControlStyle.Machine -> ToneDial(slope, onSlopeChange, label, valueDescription, modifier, range)
        else -> Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label)
                Text(valueDescription)
            }
            Slider(
                value = slope,
                onValueChange = onSlopeChange,
                valueRange = range,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { stateDescription = valueDescription },
            )
        }
    }
}

/**
 * The Machine skin's physical dial.
 *
 * Driven by a horizontal drag, never by a circular gesture: a rotary gesture
 * is unusable one-handed in the dark and impossible with a screen reader. The
 * semantics below give TalkBack a real range to announce and adjust, so the
 * skeuomorphism stays cosmetic and never costs anyone access to the control.
 */
@Composable
private fun ToneDial(
    slope: Float,
    onSlopeChange: (Float) -> Unit,
    label: String,
    valueDescription: String,
    modifier: Modifier,
    range: ClosedFloatingPointRange<Float>,
) {
    val skin = LocalSkin.current
    val density = LocalDensity.current
    val span = range.endInclusive - range.start
    val current = rememberUpdatedState(slope)
    val travelPx = remember(density) { with(density) { DIAL_TRAVEL.toPx() } }

    val draggable = rememberDraggableState { delta ->
        val next = (current.value + delta / travelPx * span).coerceIn(range.start, range.endInclusive)
        onSlopeChange(next)
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .size(DIAL_SIZE)
                .draggable(state = draggable, orientation = Orientation.Horizontal)
                .semantics {
                    contentDescription = label
                    stateDescription = valueDescription
                    progressBarRangeInfo = ProgressBarRangeInfo(slope, range)
                    setProgress { target ->
                        onSlopeChange(target.coerceIn(range.start, range.endInclusive))
                        true
                    }
                },
        ) {
            val radius = size.minDimension / 2f
            drawCircle(color = skin.palette.surfaceVariant, radius = radius)
            drawCircle(
                color = skin.palette.muted,
                radius = radius,
                style = Stroke(width = DIAL_RIM_STROKE),
            )
            val fraction = (slope - range.start) / span
            val angle = (DIAL_START_DEGREES + fraction * DIAL_SWEEP_DEGREES) * PI.toFloat() / HALF_TURN
            drawLine(
                color = skin.palette.primary,
                start = center,
                end = Offset(
                    center.x + cos(angle) * radius * DIAL_POINTER_LENGTH,
                    center.y + sin(angle) * radius * DIAL_POINTER_LENGTH,
                ),
                strokeWidth = DIAL_POINTER_STROKE,
            )
        }
        Text(valueDescription, Modifier.padding(top = 8.dp))
    }
}

private val DEFAULT_RANGE = -12f..6f
private val DIAL_SIZE = 160.dp
private val DIAL_TRAVEL = 220.dp
private const val DIAL_RIM_STROKE = 3f
private const val DIAL_POINTER_STROKE = 6f
private const val DIAL_POINTER_LENGTH = 0.66f
private const val DIAL_START_DEGREES = 140f
private const val DIAL_SWEEP_DEGREES = 260f
private const val HALF_TURN = 180f
