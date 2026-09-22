package app.noiseflow.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.noiseflow.designsystem.theme.ControlStyle
import app.noiseflow.designsystem.theme.LocalSkin

/**
 * The primary transport.
 *
 * Deliberately enormous. This is the one control people press while lying
 * down, half asleep, without their glasses, so it is far larger than the 48 dp
 * minimum and always sits in the lower half of the screen.
 */
@Composable
fun PlayControl(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    playLabel: String,
    pauseLabel: String,
    modifier: Modifier = Modifier,
) {
    val skin = LocalSkin.current
    val shape = when (skin.control) {
        ControlStyle.Machine -> RoundedCornerShape(TOGGLE_CORNER)
        else -> RoundedCornerShape(percent = 50)
    }
    Button(
        onClick = onToggle,
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = skin.palette.primary),
        modifier = modifier.size(PLAY_SIZE),
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) pauseLabel else playLabel,
            modifier = Modifier.size(PLAY_ICON_SIZE),
        )
    }
}

/** Labelled slider used for levels, width, room and the like. */
@Composable
fun LabelledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueDescription: String,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(valueDescription)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = valueDescription },
        )
    }
}

/** Row of choices rendered as a segmented control. */
@Composable
fun <T> OptionRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Button(
                onClick = { onSelect(option) },
                colors = if (isSelected) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Text(labelFor(option))
            }
        }
    }
}

private val PLAY_SIZE = 112.dp
private val PLAY_ICON_SIZE = 48.dp
private val TOGGLE_CORNER = 12.dp
