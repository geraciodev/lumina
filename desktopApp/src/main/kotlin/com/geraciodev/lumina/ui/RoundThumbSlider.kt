package com.geraciodev.lumina.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * [Slider] con un thumb circular y compacto en vez del "pill" fino y alargado por defecto de
 * Material3, más cómodo de tomar con el mouse. Usado en todos los sliders de la app (volumen,
 * barra de progreso, ajustes de proyección) para que se vean y se sientan igual.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoundThumbSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    thumbSize: Dp = 18.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        colors = colors,
        interactionSource = interactionSource,
        modifier = modifier,
        thumb = { sliderState ->
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                sliderState = sliderState,
                colors = colors,
                thumbSize = DpSize(thumbSize, thumbSize)
            )
        }
    )
}
